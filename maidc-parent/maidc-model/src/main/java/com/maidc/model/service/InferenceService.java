package com.maidc.model.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.model.dto.InferenceRequestDTO;
import com.maidc.model.entity.DeploymentEntity;
import com.maidc.model.entity.InferenceLogEntity;
import com.maidc.model.repository.DeploymentRepository;
import com.maidc.model.repository.InferenceLogRepository;
import com.maidc.model.vo.InferenceResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class InferenceService {

    private final DeploymentRepository deploymentRepository;
    private final InferenceLogRepository inferenceLogRepository;

    @Transactional
    public InferenceResultVO inference(Long deploymentId, InferenceRequestDTO dto) {
        DeploymentEntity deployment = deploymentRepository.findByIdAndIsDeletedFalse(deploymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        if (!"RUNNING".equals(deployment.getStatus())) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_NOT_RUNNING);
        }

        long startTime = System.currentTimeMillis();

        // Record inference log
        InferenceLogEntity logEntry = new InferenceLogEntity();
        logEntry.setDeploymentId(deploymentId);
        logEntry.setRequestId(dto.getRequestId());
        logEntry.setPatientId(dto.getPatientId());
        logEntry.setEncounterId(dto.getEncounterId());
        logEntry.setInputSummary(dto.getInput());
        logEntry.setOrgId(deployment.getOrgId());
        logEntry.setModelVersionNo("latest");

        try {
            // TODO: Call aiworker via Feign/HTTP for actual inference
            // Simulated result
            long latency = System.currentTimeMillis() - startTime;

            logEntry.setOutputResult(dto.getInput()); // placeholder
            logEntry.setLatencyMs((int) latency);
            logEntry.setStatus("SUCCESS");

            inferenceLogRepository.save(logEntry);

            return InferenceResultVO.builder()
                    .requestId(dto.getRequestId())
                    .results(dto.getInput()) // placeholder
                    .latencyMs(latency)
                    .modelVersion("latest")
                    .build();

        } catch (Exception e) {
            logEntry.setStatus("FAILED");
            logEntry.setErrorMessage(e.getMessage());
            inferenceLogRepository.save(logEntry);
            throw new RuntimeException("推理失败: " + e.getMessage(), e);
        }
    }
}
