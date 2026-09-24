package com.maidc.data.repository.rdr;

import com.maidc.data.entity.rdr.RdrCompletenessAssessmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RDR数据完整性评估Repository
 */
@Repository
public interface RdrCompletenessAssessmentRepository extends
        JpaRepository<RdrCompletenessAssessmentEntity, Long>,
        JpaSpecificationExecutor<RdrCompletenessAssessmentEntity> {

    Optional<RdrCompletenessAssessmentEntity> findByAssessmentCodeAndIsDeletedFalse(String assessmentCode);

    List<RdrCompletenessAssessmentEntity> findByPatientIdAndIsDeletedFalseOrderByAssessmentTimeDesc(Long patientId);

    List<RdrCompletenessAssessmentEntity> findByCohortIdAndIsDeletedFalseOrderByAssessmentTimeDesc(Long cohortId);

    boolean existsByAssessmentCodeAndIsDeletedFalse(String assessmentCode);

    @Query("SELECT a FROM RdrCompletenessAssessmentEntity a WHERE a.patientId = ?1 " +
           "AND a.status = 'COMPLETED' AND a.isDeleted = false ORDER BY a.assessmentTime DESC LIMIT 1")
    Optional<RdrCompletenessAssessmentEntity> findLatestCompletedAssessment(Long patientId);

    @Query("SELECT AVG(a.overallScore) FROM RdrCompletenessAssessmentEntity a WHERE a.status = 'COMPLETED'")
    Double getAverageOverallScore();

    @Query("SELECT a.completenessLevel, COUNT(a) FROM RdrCompletenessAssessmentEntity a " +
           "WHERE a.status = 'COMPLETED' AND a.isDeleted = false GROUP BY a.completenessLevel")
    List<Object[]> countByCompletenessLevel();

    @Query("SELECT COUNT(a) FROM RdrCompletenessAssessmentEntity a WHERE a.criticalElementsMissing > 0")
    long countWithCriticalMissing();

    Optional<RdrCompletenessAssessmentEntity> findByIdAndIsDeletedFalse(Long id);
}