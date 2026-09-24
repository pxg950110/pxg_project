package com.maidc.data.service.rdr;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.rdr.RdrExtractionTaskEntity;
import com.maidc.data.repository.rdr.RdrExtractionTaskRepository;
import com.maidc.data.repository.rdr.RdrExtractionTaskSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RDR数据抽取任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RdrExtractionTaskService {

    private final RdrExtractionTaskRepository taskRepository;

    public Page<RdrExtractionTaskEntity> list(Long projectId, String extractionType,
                                               String status, String keyword, int page, int size) {
        var spec = RdrExtractionTaskSpecification.buildSearchSpec(projectId, extractionType, status, keyword);
        return taskRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public RdrExtractionTaskEntity getById(Long id) {
        return taskRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "抽取任务不存在: " + id));
    }

    public RdrExtractionTaskEntity getByCode(String taskCode) {
        return taskRepository.findByTaskCodeAndIsDeletedFalse(taskCode)
                .orElseThrow(() -> new BusinessException(404, "抽取任务不存在: " + taskCode));
    }

    public List<RdrExtractionTaskEntity> getByProject(Long projectId) {
        return taskRepository.findByProjectIdAndIsDeletedFalse(projectId);
    }

    public List<RdrExtractionTaskEntity> getActiveScheduledTasks() {
        return taskRepository.findScheduledActiveTasks();
    }

    @Transactional
    public RdrExtractionTaskEntity create(RdrExtractionTaskEntity entity) {
        if (entity.getTaskCode() == null || entity.getTaskCode().isBlank()) {
            entity.setTaskCode("EXT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (taskRepository.existsByTaskCodeAndIsDeletedFalse(entity.getTaskCode())) {
            throw new BusinessException(400, "任务编码已存在: " + entity.getTaskCode());
        }
        if (entity.getExtractionType() == null) entity.setExtractionType("CONDITION_BASED");
        if (entity.getSourceSchema() == null) entity.setSourceSchema("cdr");
        if (entity.getOutputFormat() == null) entity.setOutputFormat("TABLE");
        if (entity.getScheduleType() == null) entity.setScheduleType("MANUAL");
        if (entity.getAutoPublish() == null) entity.setAutoPublish(false);
        if (entity.getDataVersioning() == null) entity.setDataVersioning(true);
        if (entity.getStatus() == null) entity.setStatus("DRAFT");
        if (entity.getExecutionCount() == null) entity.setExecutionCount(0);
        entity.setOrgId(0L);
        RdrExtractionTaskEntity saved = taskRepository.save(entity);
        log.info("抽取任务创建: id={}, code={}", saved.getId(), saved.getTaskCode());
        return saved;
    }

    @Transactional
    public RdrExtractionTaskEntity update(Long id, RdrExtractionTaskEntity updates) {
        RdrExtractionTaskEntity entity = getById(id);
        if (!entity.getStatus().equals("DRAFT") && !entity.getStatus().equals("CONFIGURED")) {
            throw new BusinessException(400, "只有DRAFT或CONFIGURED状态的任务可以修改");
        }
        if (updates.getTaskName() != null) entity.setTaskName(updates.getTaskName());
        if (updates.getSourceTables() != null) entity.setSourceTables(updates.getSourceTables());
        if (updates.getJoinStrategy() != null) entity.setJoinStrategy(updates.getJoinStrategy());
        if (updates.getInclusionCriteria() != null) entity.setInclusionCriteria(updates.getInclusionCriteria());
        if (updates.getExclusionCriteria() != null) entity.setExclusionCriteria(updates.getExclusionCriteria());
        if (updates.getTimeRangeStart() != null) entity.setTimeRangeStart(updates.getTimeRangeStart());
        if (updates.getTimeRangeEnd() != null) entity.setTimeRangeEnd(updates.getTimeRangeEnd());
        if (updates.getOutputFormat() != null) entity.setOutputFormat(updates.getOutputFormat());
        if (updates.getOutputSchema() != null) entity.setOutputSchema(updates.getOutputSchema());
        if (updates.getOutputTablePrefix() != null) entity.setOutputTablePrefix(updates.getOutputTablePrefix());
        if (updates.getScheduleType() != null) entity.setScheduleType(updates.getScheduleType());
        if (updates.getScheduleExpression() != null) entity.setScheduleExpression(updates.getScheduleExpression());
        if (updates.getAutoPublish() != null) entity.setAutoPublish(updates.getAutoPublish());
        if (updates.getDataVersioning() != null) entity.setDataVersioning(updates.getDataVersioning());
        if (updates.getDescription() != null) entity.setDescription(updates.getDescription());
        return taskRepository.save(entity);
    }

    @Transactional
    public RdrExtractionTaskEntity activate(Long id) {
        RdrExtractionTaskEntity entity = getById(id);
        if (!entity.getStatus().equals("DRAFT") && !entity.getStatus().equals("CONFIGURED")) {
            throw new BusinessException(400, "只有DRAFT或CONFIGURED状态的任务可以激活");
        }
        entity.setStatus("ACTIVE");
        log.info("抽取任务激活: id={}", id);
        return taskRepository.save(entity);
    }

    @Transactional
    public RdrExtractionTaskEntity pause(Long id) {
        RdrExtractionTaskEntity entity = getById(id);
        if (!entity.getStatus().equals("ACTIVE")) {
            throw new BusinessException(400, "只有ACTIVE状态的任务可以暂停");
        }
        entity.setStatus("PAUSED");
        log.info("抽取任务暂停: id={}", id);
        return taskRepository.save(entity);
    }

    @Transactional
    public RdrExtractionTaskEntity archive(Long id) {
        RdrExtractionTaskEntity entity = getById(id);
        entity.setStatus("ARCHIVED");
        log.info("抽取任务归档: id={}", id);
        return taskRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        RdrExtractionTaskEntity entity = getById(id);
        if (entity.getStatus().equals("ACTIVE")) {
            throw new BusinessException(400, "ACTIVE状态的任务不能删除，请先暂停或归档");
        }
        taskRepository.delete(entity);
        log.info("抽取任务删除: id={}", id);
    }

    @Transactional
    public void recordExecution(Long taskId, String executionStatus) {
        taskRepository.updateExecutionStats(taskId, executionStatus);
    }

    public Map<String, Object> getStats() {
        long total = taskRepository.count();
        long active = taskRepository.countActiveTasks();
        List<String> types = taskRepository.findDistinctExtractionTypes();
        return Map.of("total", total, "active", active, "types", types);
    }
}