package com.maidc.data.repository;

import com.maidc.data.entity.FollowupTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowupTaskRepository extends JpaRepository<FollowupTaskEntity, Long> {

    Optional<FollowupTaskEntity> findByIdAndIsDeletedFalse(Long id);

    List<FollowupTaskEntity> findByFollowupIdAndIsDeletedFalseOrderByDueDateAsc(Long followupId);

    List<FollowupTaskEntity> findByFollowupIdInAndStatusAndIsDeletedFalse(List<Long> followupIds, String status);

    List<FollowupTaskEntity> findByFollowupIdAndStatusAndDueDateLessThanEqualAndIsDeletedFalse(Long followupId, String status, java.time.LocalDate dueDate);

    List<FollowupTaskEntity> findByFollowupIdInAndStatusAndDueDateLessThanEqualAndIsDeletedFalse(List<Long> followupIds, String status, java.time.LocalDate dueDate);

    List<FollowupTaskEntity> findByFollowupIdAndDueDateGreaterThanEqualAndIsDeletedFalse(Long followupId, java.time.LocalDate dueDate);

    /** 本周完成（DONE by me since 周一），工作台"本周完成随访"卡片用 */
    List<FollowupTaskEntity> findByStatusAndCompletedByAndCompletedAtGreaterThanEqualAndIsDeletedFalse(
            String status, Long completedBy, java.time.LocalDateTime since);
}
