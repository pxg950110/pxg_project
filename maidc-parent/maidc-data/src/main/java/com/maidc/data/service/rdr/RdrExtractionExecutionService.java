package com.maidc.data.service.rdr;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.rdr.RdrExtractionExecutionEntity;
import com.maidc.data.entity.rdr.RdrExtractionTaskEntity;
import com.maidc.data.repository.rdr.RdrExtractionExecutionRepository;
import com.maidc.data.repository.rdr.RdrExtractionExecutionSpecification;
import com.maidc.data.repository.rdr.RdrExtractionTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RDR抽取任务执行服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RdrExtractionExecutionService {

    private final RdrExtractionExecutionRepository executionRepository;
    private final RdrExtractionTaskRepository taskRepository;

    public Page<RdrExtractionExecutionEntity> list(Long taskId, String status,
                                                     String executionType, int page, int size) {
        var spec = RdrExtractionExecutionSpecification.buildSearchSpec(taskId, status, executionType);
        return executionRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "startedAt")));
    }

    public RdrExtractionExecutionEntity getById(Long id) {
        return executionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "执行记录不存在: " + id));
    }

    public RdrExtractionExecutionEntity getByCode(String executionCode) {
        return executionRepository.findByExecutionCodeAndIsDeletedFalse(executionCode)
                .orElseThrow(() -> new BusinessException(404, "执行记录不存在: " + executionCode));
    }

    public List<RdrExtractionExecutionEntity> getByTask(Long taskId) {
        return executionRepository.findByTaskIdAndIsDeletedFalseOrderByStartedAtDesc(taskId);
    }

    public RdrExtractionExecutionEntity getLatestSuccess(Long taskId) {
        return executionRepository.findLatestSuccessExecution(taskId).orElse(null);
    }

    public List<RdrExtractionExecutionEntity> getRunningExecutions() {
        return executionRepository.findRunningExecutions();
    }

    @Transactional
    public RdrExtractionExecutionEntity startExecution(Long taskId, String executionType, String triggeredBy) {
        RdrExtractionTaskEntity task = taskRepository.findByIdAndIsDeletedFalse(taskId)
                .orElseThrow(() -> new BusinessException(404, "抽取任务不存在: " + taskId));
        if (!task.getStatus().equals("ACTIVE")) {
            throw new BusinessException(400, "任务未激活，无法执行");
        }

        RdrExtractionExecutionEntity execution = new RdrExtractionExecutionEntity();
        execution.setExecutionCode("EXE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        execution.setTaskId(taskId);
        execution.setExecutionType(executionType != null ? executionType : "MANUAL");
        execution.setStartedAt(LocalDateTime.now());
        execution.setStatus("RUNNING");
        execution.setProgressPercent(BigDecimal.ZERO);
        execution.setSourceRecordCount(0L);
        execution.setExtractedPatientCount(0L);
        execution.setExtractedRecordCount(0L);
        execution.setRetryCount(0);
        execution.setTriggeredBy(triggeredBy != null ? triggeredBy : "system");
        execution.setOrgId(0L);

        RdrExtractionExecutionEntity saved = executionRepository.save(execution);
        log.info("抽取任务启动执行: taskId={}, executionId={}", taskId, saved.getId());
        return saved;
    }

    @Transactional
    public void updateProgress(Long executionId, BigDecimal progress, String currentStep,
                                Long sourceRecordCount, Long patientCount, Long recordCount) {
        RdrExtractionExecutionEntity execution = getById(executionId);
        if (!execution.getStatus().equals("RUNNING")) {
            throw new BusinessException(400, "只有RUNNING状态的执行可以更新进度");
        }
        execution.setProgressPercent(progress);
        execution.setCurrentStep(currentStep);
        execution.setSourceRecordCount(sourceRecordCount);
        execution.setExtractedPatientCount(patientCount);
        execution.setExtractedRecordCount(recordCount);
        executionRepository.save(execution);
    }

    @Transactional
    public void completeExecution(Long executionId, String outputTableName, String outputPath,
                                   Long outputSize, Long datasetVersionId) {
        RdrExtractionExecutionEntity execution = getById(executionId);
        execution.setStatus("SUCCESS");
        execution.setFinishedAt(LocalDateTime.now());
        if (execution.getStartedAt() != null && execution.getFinishedAt() != null) {
            execution.setDurationMs(Duration.between(execution.getStartedAt(), execution.getFinishedAt()).toMillis());
        }
        execution.setProgressPercent(new BigDecimal("100"));
        execution.setOutputTableName(outputTableName);
        execution.setOutputPath(outputPath);
        execution.setOutputSizeBytes(outputSize);
        execution.setDatasetVersionId(datasetVersionId);
        executionRepository.save(execution);

        // 更新任务状态
        taskRepository.updateExecutionStats(execution.getTaskId(), "SUCCESS");
        log.info("抽取执行完成: executionId={}, patients={}, records={}", executionId,
                execution.getExtractedPatientCount(), execution.getExtractedRecordCount());
    }

    @Transactional
    public void failExecution(Long executionId, String errorMessage, String errorStack) {
        RdrExtractionExecutionEntity execution = getById(executionId);
        execution.setStatus("FAILED");
        execution.setFinishedAt(LocalDateTime.now());
        execution.setErrorMessage(errorMessage);
        execution.setErrorStack(errorStack);
        executionRepository.save(execution);

        taskRepository.updateExecutionStats(execution.getTaskId(), "FAILED");
        log.error("抽取执行失败: executionId={}, error={}", executionId, errorMessage);
    }

    @Transactional
    public RdrExtractionExecutionEntity retry(Long failedExecutionId, String triggeredBy) {
        RdrExtractionExecutionEntity failed = getById(failedExecutionId);
        if (!failed.getStatus().equals("FAILED")) {
            throw new BusinessException(400, "只有FAILED状态的执行可以重试");
        }

        RdrExtractionExecutionEntity retry = startExecution(failed.getTaskId(), "RETRY", triggeredBy);
        retry.setRetryCount(failed.getRetryCount() + 1);
        retry.setParentExecutionId(failedExecutionId);
        executionRepository.save(retry);

        failed.setRetryCount(failed.getRetryCount() + 1);
        executionRepository.save(failed);

        log.info("抽取执行重试: failedExecutionId={}, newExecutionId={}", failedExecutionId, retry.getId());
        return retry;
    }

    @Transactional
    public void cancel(Long executionId) {
        RdrExtractionExecutionEntity execution = getById(executionId);
        if (!execution.getStatus().equals("RUNNING")) {
            throw new BusinessException(400, "只有RUNNING状态的执行可以取消");
        }
        execution.setStatus("CANCELLED");
        execution.setFinishedAt(LocalDateTime.now());
        executionRepository.save(execution);
        log.info("抽取执行取消: executionId={}", executionId);
    }

    public Map<String, Object> getStats() {
        long success = executionRepository.countSuccessExecutions();
        long failed = executionRepository.countFailedExecutions();
        Double avgDuration = executionRepository.getAverageDuration();
        return Map.of(
            "success", success,
            "failed", failed,
            "averageDurationMs", avgDuration != null ? avgDuration : 0
        );
    }
}