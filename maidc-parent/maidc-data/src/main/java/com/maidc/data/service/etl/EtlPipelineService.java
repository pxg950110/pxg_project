package com.maidc.data.service.etl;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.data.dto.etl.EtlPipelineCreateDTO;
import com.maidc.data.dto.etl.EtlPipelineQueryDTO;
import com.maidc.data.entity.EtlPipelineEntity;
import com.maidc.data.entity.EtlStepEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.EtlPipelineRepository;
import com.maidc.data.repository.EtlStepRepository;
import com.maidc.data.vo.EtlPipelineDetailVO;
import com.maidc.data.vo.EtlPipelineVO;
import com.maidc.data.vo.EtlStepVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlPipelineService {

    private final EtlPipelineRepository pipelineRepository;
    private final EtlStepRepository stepRepository;
    private final DataMapper dataMapper;

    @Transactional
    public EtlPipelineVO createPipeline(EtlPipelineCreateDTO dto) {
        EtlPipelineEntity entity = new EtlPipelineEntity();
        entity.setPipelineName(dto.getPipelineName());
        entity.setSourceId(dto.getSourceId());
        entity.setDescription(dto.getDescription());
        entity.setEngineType(dto.getEngineType() != null ? dto.getEngineType() : "EMBULK");
        entity.setSyncMode(dto.getSyncMode() != null ? dto.getSyncMode() : "MANUAL");
        entity.setCronExpression(dto.getCronExpression());
        entity.setStatus("DRAFT");

        entity = pipelineRepository.save(entity);
        log.info("ETL pipeline created: id={}, name={}", entity.getId(), entity.getPipelineName());
        return enrichPipelineVO(entity);
    }

    @Transactional
    public EtlPipelineVO updatePipeline(Long id, EtlPipelineCreateDTO dto) {
        EtlPipelineEntity entity = pipelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        entity.setPipelineName(dto.getPipelineName());
        entity.setSourceId(dto.getSourceId());
        entity.setDescription(dto.getDescription());
        if (dto.getEngineType() != null) {
            entity.setEngineType(dto.getEngineType());
        }
        if (dto.getSyncMode() != null) {
            entity.setSyncMode(dto.getSyncMode());
        }
        entity.setCronExpression(dto.getCronExpression());

        entity = pipelineRepository.save(entity);
        log.info("ETL pipeline updated: id={}", id);
        return enrichPipelineVO(entity);
    }

    public EtlPipelineDetailVO getPipelineDetail(Long id) {
        EtlPipelineEntity entity = pipelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        List<EtlStepEntity> steps = stepRepository.findByPipelineIdAndIsDeletedFalseOrderByStepOrder(id);
        List<EtlStepVO> stepVOs = steps.stream()
                .map(dataMapper::toEtlStepVO)
                .toList();

        return EtlPipelineDetailVO.builder()
                .id(entity.getId())
                .pipelineName(entity.getPipelineName())
                .sourceId(entity.getSourceId())
                .description(entity.getDescription())
                .engineType(entity.getEngineType())
                .status(entity.getStatus())
                .syncMode(entity.getSyncMode())
                .cronExpression(entity.getCronExpression())
                .lastRunTime(entity.getLastRunTime())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .steps(stepVOs)
                .build();
    }

    public PageResult<EtlPipelineVO> listPipelines(EtlPipelineQueryDTO query) {
        Specification<EtlPipelineEntity> spec = (root, q, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("pipelineName")),
                        "%" + query.getKeyword().toLowerCase() + "%"));
            }
            if (query.getSourceId() != null) {
                predicates.add(cb.equal(root.get("sourceId"), query.getSourceId()));
            }
            if (query.getStatus() != null && !query.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getEngineType() != null && !query.getEngineType().isBlank()) {
                predicates.add(cb.equal(root.get("engineType"), query.getEngineType()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(
                query.getPage() - 1, query.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<EtlPipelineEntity> page = pipelineRepository.findAll(spec, pageRequest);
        Page<EtlPipelineVO> voPage = page.map(this::enrichPipelineVO);
        return PageResult.of(voPage);
    }

    @Transactional
    public void deletePipeline(Long id) {
        EtlPipelineEntity entity = pipelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        pipelineRepository.delete(entity);
        log.info("ETL pipeline soft-deleted: id={}", id);
    }

    @Transactional
    public EtlPipelineVO updateStatus(Long id, String status) {
        EtlPipelineEntity entity = pipelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        entity.setStatus(status);
        entity = pipelineRepository.save(entity);
        log.info("ETL pipeline status updated: id={}, status={}", id, status);
        return enrichPipelineVO(entity);
    }

    @Transactional
    public EtlPipelineVO copyPipeline(Long id) {
        EtlPipelineEntity source = pipelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        EtlPipelineEntity copy = new EtlPipelineEntity();
        copy.setPipelineName(source.getPipelineName() + " (Copy)");
        copy.setSourceId(source.getSourceId());
        copy.setDescription(source.getDescription());
        copy.setEngineType(source.getEngineType());
        copy.setStatus("DRAFT");
        copy.setSyncMode(source.getSyncMode());
        copy.setCronExpression(source.getCronExpression());

        copy = pipelineRepository.save(copy);
        log.info("ETL pipeline copied: fromId={}, toId={}", id, copy.getId());
        return enrichPipelineVO(copy);
    }

    public List<String> validatePipeline(Long id) {
        List<String> errors = new ArrayList<>();

        EtlPipelineEntity entity = pipelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        List<EtlStepEntity> steps = stepRepository.findByPipelineIdAndIsDeletedFalseOrderByStepOrder(id);
        if (steps.isEmpty()) {
            errors.add("Pipeline has no steps configured");
        }

        for (EtlStepEntity step : steps) {
            if (step.getSourceTable() == null || step.getSourceTable().isBlank()) {
                errors.add("Step '" + step.getStepName() + "' is missing sourceTable");
            }
            if (step.getTargetTable() == null || step.getTargetTable().isBlank()) {
                errors.add("Step '" + step.getStepName() + "' is missing targetTable");
            }
        }

        return errors;
    }

    private EtlPipelineVO enrichPipelineVO(EtlPipelineEntity entity) {
        EtlPipelineVO vo = dataMapper.toEtlPipelineVO(entity);
        long stepCount = stepRepository.countByPipelineIdAndIsDeletedFalse(entity.getId());
        vo.setStepCount((int) stepCount);
        return vo;
    }
}
