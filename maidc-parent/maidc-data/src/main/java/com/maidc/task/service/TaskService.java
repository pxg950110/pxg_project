package com.maidc.task.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.task.dto.TaskCreateDTO;
import com.maidc.task.dto.TaskUpdateDTO;
import com.maidc.task.entity.TaskEntity;
import com.maidc.task.entity.TaskExecutionEntity;
import com.maidc.task.mapper.TaskMapper;
import com.maidc.task.repository.TaskExecutionRepository;
import com.maidc.task.repository.TaskRepository;
import com.maidc.task.vo.TaskExecutionVO;
import com.maidc.task.vo.TaskVO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskExecutionRepository executionRepository;
    private final TaskMapper taskMapper;

    @Transactional
    public TaskVO createTask(TaskCreateDTO dto) {
        TaskEntity entity = taskMapper.toEntity(dto);
        entity.setStatus("CREATED");
        entity.setFailureCount(0);
        entity.setIsDeleted(false);
        TaskEntity saved = taskRepository.save(entity);
        log.info("Task created: id={}, name={}", saved.getId(), saved.getTaskName());
        return taskMapper.toVO(saved);
    }

    @Transactional
    public TaskVO updateTask(String id, TaskUpdateDTO dto) {
        TaskEntity entity = getTaskOrThrow(id);
        taskMapper.updateEntity(dto, entity);
        TaskEntity saved = taskRepository.save(entity);
        return taskMapper.toVO(saved);
    }

    @Transactional
    public void deleteTask(String id) {
        TaskEntity entity = getTaskOrThrow(id);
        entity.setIsDeleted(true);
        taskRepository.save(entity);
    }

    public PageResult<TaskVO> listTasks(String status, String taskType, int page, int pageSize) {
        Specification<TaskEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (taskType != null) predicates.add(cb.equal(root.get("taskType"), taskType));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<TaskEntity> result = taskRepository.findAll(spec,
                PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(result.map(taskMapper::toVO));
    }

    public TaskVO getTask(String id) {
        return taskMapper.toVO(getTaskOrThrow(id));
    }

    @Transactional
    public TaskVO triggerTask(String id) {
        TaskEntity entity = getTaskOrThrow(id);
        log.info("Task triggered manually: id={}, name={}", id, entity.getTaskName());

        // Create execution record
        TaskExecutionEntity execution = new TaskExecutionEntity();
        execution.setTaskId(id);
        execution.setStatus("RUNNING");
        execution.setStartTime(LocalDateTime.now());
        execution.setTriggerType("MANUAL");
        executionRepository.save(execution);

        entity.setStatus("RUNNING");
        entity.setLastExecutionTime(LocalDateTime.now());
        taskRepository.save(entity);

        return taskMapper.toVO(entity);
    }

    @Transactional
    public TaskVO pauseTask(String id) {
        TaskEntity entity = getTaskOrThrow(id);
        if (!"RUNNING".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_NOT_RUNNING);
        }
        entity.setStatus("PAUSED");
        taskRepository.save(entity);
        return taskMapper.toVO(entity);
    }

    @Transactional
    public TaskVO resumeTask(String id) {
        TaskEntity entity = getTaskOrThrow(id);
        if (!"PAUSED".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_NOT_PAUSED);
        }
        entity.setStatus("RUNNING");
        taskRepository.save(entity);
        return taskMapper.toVO(entity);
    }

    public PageResult<TaskExecutionVO> getExecutions(String taskId, int page, int pageSize) {
        Page<TaskExecutionEntity> result = executionRepository.findByTaskIdOrderByStartTimeDesc(
                taskId, PageRequest.of(page - 1, pageSize));
        return PageResult.of(result.map(taskMapper::toExecutionVO));
    }

    private TaskEntity getTaskOrThrow(String id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
