package com.maidc.data.repository;

import com.maidc.data.entity.ScaleAssessmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScaleAssessmentRepository extends JpaRepository<ScaleAssessmentEntity, Long> {

    List<ScaleAssessmentEntity> findByFollowupIdAndIsDeletedFalseOrderByAssessedAtAsc(Long followupId);

    List<ScaleAssessmentEntity> findByFollowupIdAndScaleCodeAndIsDeletedFalseOrderByAssessedAtAsc(Long followupId, String scaleCode);

    List<ScaleAssessmentEntity> findByFollowupIdInAndScaleCodeAndIsDeletedFalseOrderByAssessedAtAsc(List<Long> followupIds, String scaleCode);

    boolean existsByTaskIdAndScaleCodeAndIsDeletedFalse(Long taskId, String scaleCode);

    List<ScaleAssessmentEntity> findByTaskIdAndIsDeletedFalse(Long taskId);
}
