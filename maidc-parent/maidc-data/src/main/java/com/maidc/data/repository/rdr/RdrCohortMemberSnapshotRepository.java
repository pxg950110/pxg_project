package com.maidc.data.repository.rdr;

import com.maidc.data.entity.rdr.RdrCohortMemberSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * RDR队列成员快照Repository
 */
@Repository
public interface RdrCohortMemberSnapshotRepository extends
        JpaRepository<RdrCohortMemberSnapshotEntity, Long>,
        JpaSpecificationExecutor<RdrCohortMemberSnapshotEntity> {

    List<RdrCohortMemberSnapshotEntity> findByResultIdAndIsActiveAndIsDeletedFalse(Long resultId, Boolean isActive);

    List<RdrCohortMemberSnapshotEntity> findByCohortIdAndIsActiveAndIsDeletedFalse(Long cohortId, Boolean isActive);

    List<RdrCohortMemberSnapshotEntity> findByPatientIdAndCohortIdAndIsDeletedFalse(Long patientId, Long cohortId);

    boolean existsByResultIdAndPatientIdAndIsDeletedFalse(Long resultId, Long patientId);

    @Modifying
    @Query("UPDATE RdrCohortMemberSnapshotEntity m SET m.isActive = false, m.removedAt = CURRENT_TIMESTAMP, " +
           "m.removedReason = ?2 WHERE m.resultId = ?1 AND m.patientId = ?3")
    void removeMember(Long resultId, String reason, Long patientId);

    @Query("SELECT COUNT(m) FROM RdrCohortMemberSnapshotEntity m WHERE m.resultId = ?1 AND m.isActive = true")
    long countActiveMembersByResult(Long resultId);

    @Query("SELECT COUNT(m) FROM RdrCohortMemberSnapshotEntity m WHERE m.cohortId = ?1 AND m.isActive = true")
    long countActiveMembersByCohort(Long cohortId);

    @Query("SELECT AVG(m.dataCompleteness) FROM RdrCohortMemberSnapshotEntity m " +
           "WHERE m.resultId = ?1 AND m.isActive = true")
    Double getAverageCompleteness(Long resultId);

    @Query("SELECT m FROM RdrCohortMemberSnapshotEntity m WHERE m.resultId = ?1 " +
           "AND m.isActive = true AND m.isDeleted = false ORDER BY m.matchScore DESC LIMIT ?2")
    List<RdrCohortMemberSnapshotEntity> findTopMembersByMatchScore(Long resultId, int limit);
}