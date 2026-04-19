package com.maidc.data.service.etl;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.data.dto.etl.EtlExecutionQueryDTO;
import com.maidc.data.entity.EtlExecutionEntity;
import com.maidc.data.entity.EtlPipelineEntity;
import com.maidc.data.entity.EtlStepEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.EtlExecutionRepository;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlExecutionService {

    private final EtlPipelineRepository pipelineRepository;
    private final EtlStepRepository stepRepository;
    private final EtlExecutionRepository executionRepository;
    private final EtlConfigGenerator configGenerator;
    private final DataMapper dataMapper;

    private final Map<Long, Process> runningProcesses = new ConcurrentHashMap<>();

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

        // Load steps
        List<EtlStepEntity> steps = stepRepository.findByPipelineIdAndIsDeletedFalseOrderByStepOrder(pipelineId);

        // Start async execution thread
        Thread executorThread = new Thread(() -> executeSteps(executionId, pipelineId, steps),
                "etl-exec-" + executionId);
        executorThread.setDaemon(true);
        executorThread.start();

        log.info("ETL execution triggered: executionId={}, pipelineId={}, steps={}", executionId, pipelineId, steps.size());
        return enrichExecutionVO(execution);
    }

    /**
     * Execute all steps sequentially in background.
     */
    private void executeSteps(Long executionId, Long pipelineId, List<EtlStepEntity> steps) {
        try {
            for (EtlStepEntity step : steps) {
                EtlExecutionEntity stepExecution = executeSingleStep(executionId, pipelineId, step);

                if ("FAILED".equals(stepExecution.getStatus())) {
                    String onError = step.getOnError() != null ? step.getOnError() : "ABORT";
                    switch (onError) {
                        case "ABORT" -> {
                            markRemainingStepsSkipped(executionId, pipelineId, step.getStepOrder());
                            finalizePipelineExecution(executionId, pipelineId, "FAILED");
                            return;
                        }
                        case "RETRY" -> {
                            boolean succeeded = retryStep(executionId, pipelineId, step, 3);
                            if (!succeeded) {
                                markRemainingStepsSkipped(executionId, pipelineId, step.getStepOrder());
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
    private EtlExecutionEntity executeSingleStep(Long parentExecutionId, Long pipelineId, EtlStepEntity step) {
        Long orgId = pipelineRepository.findById(pipelineId)
                .map(EtlPipelineEntity::getOrgId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // Create step-level execution record
        EtlExecutionEntity stepExecution = new EtlExecutionEntity();
        stepExecution.setOrgId(orgId);
        stepExecution.setPipelineId(pipelineId);
        stepExecution.setStepId(step.getId());
        stepExecution.setStatus("RUNNING");
        stepExecution.setStartTime(LocalDateTime.now());
        stepExecution.setTriggerType("AUTO");
        stepExecution = executionRepository.save(stepExecution);

        try {
            // Generate Embulk config
            // Use connection params from DataSource (simplified: using defaults for now)
            String config = configGenerator.generateEmbulkConfig(
                    step,
                    "localhost", 5432, "source_db", "source_user", "source_pass",
                    "localhost", 5432, "target_db", "target_user", "target_pass"
            );

            stepExecution.setEngineConfig(config);
            stepExecution = executionRepository.save(stepExecution);

            // Execute Embulk via ProcessBuilder
            ProcessBuilder pb = new ProcessBuilder("embulk", "run", "-");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            runningProcesses.put(stepExecution.getId(), process);

            // Write config to stdin
            try (var os = process.getOutputStream()) {
                os.write(config.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // Capture output for logging
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            runningProcesses.remove(stepExecution.getId());

            if (exitCode == 0) {
                stepExecution.setStatus("SUCCESS");
                // Update step lastSyncTime
                step.setLastSyncTime(LocalDateTime.now());
                stepRepository.save(step);
                log.info("Step {} executed successfully", step.getId());
            } else {
                stepExecution.setStatus("FAILED");
                stepExecution.setErrorMessage("Embulk exited with code " + exitCode + ": " +
                        truncate(output.toString(), 4000));
                log.error("Step {} failed with exit code {}", step.getId(), exitCode);
            }

        } catch (Exception e) {
            runningProcesses.remove(stepExecution.getId());
            stepExecution.setStatus("FAILED");
            stepExecution.setErrorMessage(truncate(e.getMessage(), 4000));
            log.error("Step {} execution error", step.getId(), e);
        }

        stepExecution.setEndTime(LocalDateTime.now());
        stepExecution = executionRepository.save(stepExecution);
        return stepExecution;
    }

    /**
     * Retry a step up to maxRetries times.
     */
    private boolean retryStep(Long executionId, Long pipelineId, EtlStepEntity step, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            log.info("Retrying step {} (attempt {}/{})", step.getId(), attempt, maxRetries);
            EtlExecutionEntity retryExecution = executeSingleStep(executionId, pipelineId, step);
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
    private void markRemainingStepsSkipped(Long executionId, Long pipelineId, int afterStepOrder) {
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
        Specification<EtlExecutionEntity> spec = (root, q, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            if (query.getPipelineId() != null) {
                predicates.add(cb.equal(root.get("pipelineId"), query.getPipelineId()));
            }
            if (query.getStepId() != null) {
                predicates.add(cb.equal(root.get("stepId"), query.getStepId()));
            }
            if (query.getStatus() != null && !query.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getTriggerType() != null && !query.getTriggerType().isBlank()) {
                predicates.add(cb.equal(root.get("triggerType"), query.getTriggerType()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(
                query.getPage() - 1, query.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<EtlExecutionEntity> page = executionRepository.findAll(spec, pageRequest);
        Page<EtlExecutionVO> voPage = page.map(this::enrichExecutionVO);
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

        // Kill process if running
        Process process = runningProcesses.remove(id);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            log.info("Killed process for execution {}", id);
        }

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

    /**
     * Enrich execution VO with pipelineName and stepName.
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

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...(truncated)";
    }
}
