package com.maidc.data.service.etl;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.data.config.EtlProperties;
import com.maidc.data.dto.etl.EtlExecutionQueryDTO;
import com.maidc.data.entity.EtlExecutionEntity;
import com.maidc.data.entity.EtlPipelineEntity;
import com.maidc.data.entity.EtlStepEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.EtlExecutionRepository;
import com.maidc.data.repository.EtlExecutionSpecification;
import com.maidc.data.repository.EtlPipelineRepository;
import com.maidc.data.repository.EtlStepRepository;
import com.maidc.data.vo.EtlExecutionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlExecutionService {

    private static final int MAX_STEP_RETRIES = 3;

    private final EtlPipelineRepository pipelineRepository;
    private final EtlStepRepository stepRepository;
    private final EtlExecutionRepository executionRepository;
    private final EtlConfigGenerator configGenerator;
    private final EmbulkProcessRunner embulkRunner;
    private final com.maidc.data.repository.DataSourceRepository dataSourceRepository;
    private final EtlProperties etlProperties;
    private final ExecutorService etlExecutor;
    private final DataMapper dataMapper;

    /**
     * Trigger a new pipeline execution. Returns immediately; steps run in background.
     */
    @Transactional
    public EtlExecutionVO triggerExecution(Long pipelineId, String triggerType) {
        EtlPipelineEntity pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // Check if pipeline is already running
        List<EtlExecutionEntity> runningExecutions =
                executionRepository.findByPipelineIdAndStatusAndIsDeletedFalse(pipelineId, "RUNNING");
        if (!runningExecutions.isEmpty()) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_RUNNING);
        }

        // Create pipeline-level execution record
        EtlExecutionEntity execution = new EtlExecutionEntity();
        execution.setOrgId(pipeline.getOrgId());
        execution.setPipelineId(pipelineId);
        execution.setStatus("RUNNING");
        execution.setStartTime(LocalDateTime.now());
        execution.setTriggerType(triggerType != null ? triggerType : "MANUAL");
        execution = executionRepository.save(execution);

        final Long executionId = execution.getId();

        // Load steps (plain data for the background task)
        List<EtlStepEntity> steps = stepRepository.findByPipelineIdAndIsDeletedFalseOrderByStepOrder(pipelineId);

        etlExecutor.submit(() -> executeSteps(executionId, pipelineId, steps));

        log.info("ETL execution triggered: executionId={}, pipelineId={}, steps={}", executionId, pipelineId, steps.size());
        return enrichExecutionVO(execution);
    }

    /**
     * Execute all steps sequentially in background.
     */
    private void executeSteps(Long executionId, Long pipelineId, List<EtlStepEntity> steps) {
        try {
            for (EtlStepEntity step : steps) {
                EtlExecutionEntity stepExecution = executeSingleStep(pipelineId, step);

                if ("FAILED".equals(stepExecution.getStatus())) {
                    String onError = step.getOnError() != null ? step.getOnError() : "ABORT";
                    switch (onError) {
                        case "ABORT" -> {
                            markRemainingStepsSkipped(pipelineId, step.getStepOrder());
                            finalizePipelineExecution(executionId, pipelineId, "FAILED");
                            return;
                        }
                        case "RETRY" -> {
                            boolean succeeded = retryStep(pipelineId, step, MAX_STEP_RETRIES);
                            if (!succeeded) {
                                markRemainingStepsSkipped(pipelineId, step.getStepOrder());
                                finalizePipelineExecution(executionId, pipelineId, "FAILED");
                                return;
                            }
                        }
                        // SKIP: continue to next step
                        default -> log.warn("Step {} failed but onError={}, continuing", step.getId(), onError);
                    }
                }
            }

            // All steps completed
            finalizePipelineExecution(executionId, pipelineId, "SUCCESS");

        } catch (Exception e) {
            log.error("Unexpected error during execution {}", executionId, e);
            finalizePipelineExecution(executionId, pipelineId, "FAILED");
        }
    }

    /**
     * Execute a single step and return the execution record.
     */
    private EtlExecutionEntity executeSingleStep(Long pipelineId, EtlStepEntity step) {
        EtlPipelineEntity pipelineEntity = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Long orgId = pipelineEntity.getOrgId();

        // Create step-level execution record
        EtlExecutionEntity stepExecution = new EtlExecutionEntity();
        stepExecution.setOrgId(orgId);
        stepExecution.setPipelineId(pipelineId);
        stepExecution.setStepId(step.getId());
        stepExecution.setStatus("RUNNING");
        stepExecution.setStartTime(LocalDateTime.now());
        stepExecution.setTriggerType("AUTO");
        stepExecution = executionRepository.save(stepExecution);

        Long stepExecutionId = stepExecution.getId();
        try {
            ConnectionParams source = resolveConnection(
                    pipelineEntity.getSourceId() == null ? null
                            : dataSourceRepository.findById(pipelineEntity.getSourceId()).orElse(null),
                    etlProperties.getSourceHost(), etlProperties.getSourcePort(), etlProperties.getSourceDatabase(),
                    etlProperties.getSourceUser(), etlProperties.getSourcePassword());
            ConnectionParams target = new ConnectionParams(
                    etlProperties.getTargetHost(), etlProperties.getTargetPort(), etlProperties.getTargetDatabase(),
                    etlProperties.getTargetUser(), etlProperties.getTargetPassword());

            String config = configGenerator.generateEmbulkConfig(
                    step,
                    source.host(), source.port(), source.database(), source.user(), source.password(),
                    target.host(), target.port(), target.database(), target.user(), target.password()
            );

            stepExecution.setEngineConfig(config);
            stepExecution = executionRepository.save(stepExecution);

            EmbulkProcessRunner.RunResult result = embulkRunner.run(stepExecutionId, config);

            if (result.success()) {
                stepExecution.setStatus("SUCCESS");
                // Update step lastSyncTime
                step.setLastSyncTime(LocalDateTime.now());
                stepRepository.save(step);
                log.info("Step {} executed successfully", step.getId());
            } else {
                stepExecution.setStatus("FAILED");
                stepExecution.setErrorMessage("Embulk exited with code " + result.exitCode() + ": "
                        + EmbulkProcessRunner.truncateOutput(result.output()));
                log.error("Step {} failed with exit code {}", step.getId(), result.exitCode());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stepExecution.setStatus("FAILED");
            stepExecution.setErrorMessage("Embulk execution interrupted");
            log.error("Step {} execution interrupted", step.getId(), e);
        } catch (Exception e) {
            stepExecution.setStatus("FAILED");
            stepExecution.setErrorMessage(EmbulkProcessRunner.truncate(e.getMessage(), 4000));
            log.error("Step {} execution error", step.getId(), e);
        }

        stepExecution.setEndTime(LocalDateTime.now());
        return executionRepository.save(stepExecution);
    }

    /**
     * 数据源实体连接参数解析：实体字段非空优先，空缺字段逐项回落 EtlProperties 兜底值。
     */
    private ConnectionParams resolveConnection(com.maidc.data.entity.DataSourceEntity entity,
                                               String fbHost, int fbPort, String fbDb,
                                               String fbUser, String fbPass) {
        if (entity == null) {
            return new ConnectionParams(fbHost, fbPort, fbDb, fbUser, fbPass);
        }
        return new ConnectionParams(
                nonBlank(entity.getHost()) ? entity.getHost() : fbHost,
                entity.getPort() != null ? entity.getPort() : fbPort,
                nonBlank(entity.getDatabaseName()) ? entity.getDatabaseName() : fbDb,
                nonBlank(entity.getUsername()) ? entity.getUsername() : fbUser,
                entity.getPassword() != null ? entity.getPassword() : fbPass);
    }

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private record ConnectionParams(String host, int port, String database, String user, String password) {
    }

    /**
     * Retry a step up to maxRetries times.
     */
    private boolean retryStep(Long pipelineId, EtlStepEntity step, int maxRetries) {        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            log.info("Retrying step {} (attempt {}/{})", step.getId(), attempt, maxRetries);
            EtlExecutionEntity retryExecution = executeSingleStep(pipelineId, step);
            if ("SUCCESS".equals(retryExecution.getStatus())) {
                return true;
            }
        }
        log.error("Step {} failed after {} retries", step.getId(), maxRetries);
        return false;
    }

    /**
     * Mark remaining steps (with higher stepOrder) as SKIPPED.
     */
    private void markRemainingStepsSkipped(Long pipelineId, int afterStepOrder) {
        Long orgId = pipelineRepository.findById(pipelineId)
                .map(EtlPipelineEntity::getOrgId)
                .orElse(null);

        List<EtlStepEntity> remainingSteps = stepRepository.findByPipelineIdAndIsDeletedFalseOrderByStepOrder(pipelineId);
        for (EtlStepEntity s : remainingSteps) {
            if (s.getStepOrder() > afterStepOrder) {
                EtlExecutionEntity skipped = new EtlExecutionEntity();
                skipped.setOrgId(orgId);
                skipped.setPipelineId(pipelineId);
                skipped.setStepId(s.getId());
                skipped.setStatus("SKIPPED");
                skipped.setStartTime(LocalDateTime.now());
                skipped.setEndTime(LocalDateTime.now());
                skipped.setTriggerType("AUTO");
                skipped.setErrorMessage("Skipped due to previous step failure");
                executionRepository.save(skipped);
            }
        }
    }

    /**
     * Finalize the pipeline-level execution record.
     */
    private void finalizePipelineExecution(Long executionId, Long pipelineId, String status) {
        EtlExecutionEntity execution = executionRepository.findById(executionId).orElse(null);
        if (execution != null) {
            execution.setStatus(status);
            execution.setEndTime(LocalDateTime.now());
            executionRepository.save(execution);
        }

        // Update pipeline lastRunTime
        EtlPipelineEntity pipeline = pipelineRepository.findById(pipelineId).orElse(null);
        if (pipeline != null) {
            pipeline.setLastRunTime(LocalDateTime.now());
            pipelineRepository.save(pipeline);
        }

        log.info("Pipeline execution finalized: executionId={}, status={}", executionId, status);
    }

    /**
     * List executions with filtering and pagination.
     */
    public PageResult<EtlExecutionVO> listExecutions(EtlExecutionQueryDTO query) {
        Specification<EtlExecutionEntity> spec = EtlExecutionSpecification.buildSearchSpec(
                query.getPipelineId(), query.getStepId(), query.getStatus(), query.getTriggerType());

        PageRequest pageRequest = PageRequest.of(
                query.getPage() - 1, query.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<EtlExecutionEntity> page = executionRepository.findAll(spec, pageRequest);
        ExecutionNames names = loadNames(page.getContent());
        Page<EtlExecutionVO> voPage = page.map(entity -> toExecutionVO(entity, names));
        return PageResult.of(voPage);
    }

    /**
     * Get a single execution by ID.
     */
    public EtlExecutionVO getExecution(Long id) {
        EtlExecutionEntity entity = executionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return enrichExecutionVO(entity);
    }

    /**
     * Cancel a running execution by killing its process.
     */
    @Transactional
    public void cancelExecution(Long id) {
        EtlExecutionEntity entity = executionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!"RUNNING".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_NOT_RUNNING);
        }

        // Kill process if running (only effective when this instance spawned it)
        embulkRunner.cancel(id);

        entity.setStatus("CANCELLED");
        entity.setEndTime(LocalDateTime.now());
        executionRepository.save(entity);
        log.info("Execution cancelled: id={}", id);
    }

    /**
     * Retry a failed/completed execution by creating a new one for the same pipeline.
     */
    @Transactional
    public EtlExecutionVO retryExecution(Long id) {
        EtlExecutionEntity original = executionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        return triggerExecution(original.getPipelineId(), "RETRY");
    }

    /**
     * Get execution logs. Returns errorMessage if available, otherwise reads log file.
     */
    public String getExecutionLogs(Long id) {
        EtlExecutionEntity entity = executionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // Return error message if set
        if (entity.getErrorMessage() != null && !entity.getErrorMessage().isBlank()) {
            return entity.getErrorMessage();
        }

        // Try to read log file if logPath is set
        if (entity.getLogPath() != null && !entity.getLogPath().isBlank()) {
            try {
                return Files.readString(Path.of(entity.getLogPath()));
            } catch (Exception e) {
                log.warn("Failed to read log file: {}", entity.getLogPath(), e);
                return "Unable to read log file: " + entity.getLogPath();
            }
        }

        return "No logs available";
    }

    // -------------------------------------------------------------- enrichment

    /**
     * 单条 enriched 查询：逐个补齐 pipelineName / stepName。
     */
    private EtlExecutionVO enrichExecutionVO(EtlExecutionEntity entity) {
        EtlExecutionVO vo = dataMapper.toEtlExecutionVO(entity);

        if (entity.getPipelineId() != null) {
            pipelineRepository.findById(entity.getPipelineId()).ifPresent(p ->
                    vo.setPipelineName(p.getPipelineName()));
        }

        if (entity.getStepId() != null) {
            stepRepository.findById(entity.getStepId()).ifPresent(s ->
                    vo.setStepName(s.getStepName()));
        }

        return vo;
    }

    /**
     * 分页列表的名称批量加载：整页只做两次 findAllById，避免逐行查库。
     */
    private ExecutionNames loadNames(List<EtlExecutionEntity> entities) {
        List<Long> pipelineIds = entities.stream()
                .map(EtlExecutionEntity::getPipelineId).filter(Objects::nonNull).distinct().toList();
        List<Long> stepIds = entities.stream()
                .map(EtlExecutionEntity::getStepId).filter(Objects::nonNull).distinct().toList();

        Map<Long, String> pipelineNames = pipelineRepository.findAllById(pipelineIds).stream()
                .collect(Collectors.toMap(EtlPipelineEntity::getId, EtlPipelineEntity::getPipelineName));
        Map<Long, String> stepNames = stepRepository.findAllById(stepIds).stream()
                .collect(Collectors.toMap(EtlStepEntity::getId, EtlStepEntity::getStepName));
        return new ExecutionNames(pipelineNames, stepNames);
    }

    private EtlExecutionVO toExecutionVO(EtlExecutionEntity entity, ExecutionNames names) {
        EtlExecutionVO vo = dataMapper.toEtlExecutionVO(entity);
        vo.setPipelineName(names.pipelineName(entity.getPipelineId()));
        vo.setStepName(names.stepName(entity.getStepId()));
        return vo;
    }

    private record ExecutionNames(Map<Long, String> pipelineNames, Map<Long, String> stepNames) {
        String pipelineName(Long id) { return id == null ? null : pipelineNames.get(id); }

        String stepName(Long id) { return id == null ? null : stepNames.get(id); }
    }
}
