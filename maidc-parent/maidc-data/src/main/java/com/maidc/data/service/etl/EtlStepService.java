package com.maidc.data.service.etl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.dto.etl.EtlStepCreateDTO;
import com.maidc.data.dto.etl.EtlStepUpdateDTO;
import com.maidc.data.entity.EtlFieldMappingEntity;
import com.maidc.data.entity.EtlPipelineEntity;
import com.maidc.data.entity.EtlStepEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.EtlFieldMappingRepository;
import com.maidc.data.repository.EtlPipelineRepository;
import com.maidc.data.repository.EtlStepRepository;
import com.maidc.data.vo.EtlFieldMappingVO;
import com.maidc.data.vo.EtlStepVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlStepService {

    private final EtlPipelineRepository pipelineRepository;
    private final EtlStepRepository stepRepository;
    private final EtlFieldMappingRepository fieldMappingRepository;
    private final DataMapper dataMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public EtlStepVO createStep(Long pipelineId, EtlStepCreateDTO dto) {
        EtlPipelineEntity pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        EtlStepEntity entity = new EtlStepEntity();
        entity.setOrgId(pipeline.getOrgId());
        entity.setPipelineId(pipelineId);
        entity.setStepName(dto.getStepName());
        entity.setStepOrder(dto.getStepOrder());
        entity.setStepType(dto.getStepType() != null ? dto.getStepType() : "ONE_TO_ONE");
        entity.setSourceSchema(dto.getSourceSchema());
        entity.setSourceTable(dto.getSourceTable());
        entity.setTargetSchema(dto.getTargetSchema());
        entity.setTargetTable(dto.getTargetTable());
        entity.setFilterCondition(dto.getFilterCondition());
        entity.setPreSql(dto.getPreSql());
        entity.setPostSql(dto.getPostSql());
        entity.setOnError(dto.getOnError() != null ? dto.getOnError() : "ABORT");
        entity.setSyncMode(dto.getSyncMode() != null ? dto.getSyncMode() : "INCREMENTAL");

        entity.setJoinConfig(parseJson(dto.getJoinConfig()));
        entity.setTransformConfig(parseJson(dto.getTransformConfig()));

        entity = stepRepository.save(entity);
        log.info("ETL step created: id={}, pipelineId={}, name={}", entity.getId(), pipelineId, entity.getStepName());
        return dataMapper.toEtlStepVO(entity);
    }

    @Transactional
    public EtlStepVO updateStep(Long stepId, EtlStepUpdateDTO dto) {
        EtlStepEntity entity = stepRepository.findById(stepId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (dto.getStepName() != null) {
            entity.setStepName(dto.getStepName());
        }
        if (dto.getStepOrder() != null) {
            entity.setStepOrder(dto.getStepOrder());
        }
        if (dto.getStepType() != null) {
            entity.setStepType(dto.getStepType());
        }
        if (dto.getSourceSchema() != null) {
            entity.setSourceSchema(dto.getSourceSchema());
        }
        if (dto.getSourceTable() != null) {
            entity.setSourceTable(dto.getSourceTable());
        }
        if (dto.getTargetSchema() != null) {
            entity.setTargetSchema(dto.getTargetSchema());
        }
        if (dto.getTargetTable() != null) {
            entity.setTargetTable(dto.getTargetTable());
        }
        if (dto.getFilterCondition() != null) {
            entity.setFilterCondition(dto.getFilterCondition());
        }
        if (dto.getPreSql() != null) {
            entity.setPreSql(dto.getPreSql());
        }
        if (dto.getPostSql() != null) {
            entity.setPostSql(dto.getPostSql());
        }
        if (dto.getOnError() != null) {
            entity.setOnError(dto.getOnError());
        }
        if (dto.getSyncMode() != null) {
            entity.setSyncMode(dto.getSyncMode());
        }
        if (dto.getJoinConfig() != null) {
            entity.setJoinConfig(parseJson(dto.getJoinConfig()));
        }
        if (dto.getTransformConfig() != null) {
            entity.setTransformConfig(parseJson(dto.getTransformConfig()));
        }

        entity = stepRepository.save(entity);
        log.info("ETL step updated: id={}", stepId);
        return dataMapper.toEtlStepVO(entity);
    }

    public List<EtlStepVO> listSteps(Long pipelineId) {
        List<EtlStepEntity> steps = stepRepository.findByPipelineIdAndIsDeletedFalseOrderByStepOrder(pipelineId);
        return steps.stream().map(entity -> {
            EtlStepVO vo = dataMapper.toEtlStepVO(entity);
            List<EtlFieldMappingEntity> mappings =
                    fieldMappingRepository.findByStepIdAndIsDeletedFalseOrderBySortOrder(entity.getId());
            vo.setFieldMappings(mappings.stream().map(dataMapper::toEtlFieldMappingVO).toList());
            return vo;
        }).toList();
    }

    @Transactional
    public void deleteStep(Long stepId) {
        EtlStepEntity entity = stepRepository.findById(stepId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        fieldMappingRepository.deleteByStepId(stepId);
        stepRepository.delete(entity);
        log.info("ETL step deleted (cascade field mappings): id={}", stepId);
    }

    @Transactional
    public void reorderSteps(Long pipelineId, List<Long> stepIds) {
        for (int i = 0; i < stepIds.size(); i++) {
            Long stepId = stepIds.get(i);
            EtlStepEntity entity = stepRepository.findById(stepId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
            entity.setStepOrder(i + 1);
            stepRepository.save(entity);
        }
        log.info("ETL steps reordered: pipelineId={}, stepIds={}", pipelineId, stepIds);
    }

    public List<Map<String, Object>> previewData(Long stepId) {
        EtlStepEntity entity = stepRepository.findById(stepId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        String schema = entity.getSourceSchema() != null ? entity.getSourceSchema() + "." : "";
        String sql = "SELECT * FROM " + schema + entity.getSourceTable() + " LIMIT 10";
        log.info("Preview data for step {}: {}", stepId, sql);
        return jdbcTemplate.queryForList(sql);
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("Failed to parse JSON: {}", json, e);
            return null;
        }
    }
}
