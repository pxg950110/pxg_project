package com.maidc.task.repository;

import com.maidc.task.entity.TaskExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskExecutionRepository extends JpaRepository<TaskExecutionEntity, String> {

    Page<TaskExecutionEntity> findByTaskIdOrderByStartTimeDesc(String taskId, Pageable pageable);
}
