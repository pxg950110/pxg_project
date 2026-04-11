package com.maidc.model.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.model.dto.EvaluationCreateDTO;
import com.maidc.model.entity.EvaluationEntity;
import com.maidc.model.entity.ModelVersionEntity;
import com.maidc.model.mapper.ModelMapper;
import com.maidc.model.mq.ModelMessageProducer;
import com.maidc.model.repository.EvaluationRepository;
import com.maidc.model.repository.VersionRepository;
import com.maidc.model.vo.EvaluationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final VersionRepository versionRepository;
    private final ModelMessageProducer messageProducer;
    private final ModelMapper modelMapper;

    @Transactional
    public EvaluationVO createEvaluation(EvaluationCreateDTO dto) {
        ModelVersionEntity version = versionRepository.findByIdAndIsDeletedFalse(dto.getModelVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND));

        EvaluationEntity eval = new EvaluationEntity();
        eval.setModelId(version.getModelId());
        eval.setVersionId(dto.getModelVersionId());
        eval.setEvalName(dto.getEvalName());
        eval.setEvalType(dto.getEvalType());
        eval.setDatasetId(dto.getDatasetId());
        eval.setStatus("PENDING");
        eval = evaluationRepository.save(eval);

        // Send MQ message to aiworker
        Map<String, Object> metricsConfig = new HashMap<>();
        if (dto.getMetricsConfig() != null) {
            dto.getMetricsConfig().fields().forEachRemaining(entry ->
                    metricsConfig.put(entry.getKey(), entry.getValue()));
        }
        messageProducer.sendEvaluationTask(eval.getId(), dto.getModelVersionId(), dto.getDatasetId(), metricsConfig);

        // Update version status
        version.setStatus("EVALUATING");
        versionRepository.save(version);

        log.info("评估任务已创建: id={}, versionId={}", eval.getId(), dto.getModelVersionId());
        return modelMapper.toEvaluationVO(eval);
    }

    public EvaluationVO getEvaluation(Long id) {
        EvaluationEntity eval = evaluationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVALUATION_NOT_FOUND));
        return modelMapper.toEvaluationVO(eval);
    }

    public String getEvaluationReportUrl(Long id) {
        EvaluationEntity eval = evaluationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVALUATION_NOT_FOUND));
        if (!"COMPLETED".equals(eval.getStatus())) {
            throw new BusinessException(ErrorCode.EVALUATION_NOT_COMPLETED);
        }
        return eval.getReportUrl();
    }
}
