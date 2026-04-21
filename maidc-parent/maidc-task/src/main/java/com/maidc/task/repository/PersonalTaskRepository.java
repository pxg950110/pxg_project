package com.maidc.task.repository;

import com.maidc.task.entity.PersonalTaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonalTaskRepository extends JpaRepository<PersonalTaskEntity, Long> {

    List<PersonalTaskEntity> findByAssigneeIdAndStatusInOrderByPriorityDescCreatedAtDesc(
            Long assigneeId, List<String> statuses);

    Page<PersonalTaskEntity> findByAssigneeIdAndStatusInOrderByPriorityDescCreatedAtDesc(
            Long assigneeId, List<String> statuses, Pageable pageable);

    long countByAssigneeIdAndStatus(Long assigneeId, String status);
}
