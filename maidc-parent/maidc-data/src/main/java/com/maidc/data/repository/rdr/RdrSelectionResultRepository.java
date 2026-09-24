package com.maidc.data.repository.rdr;

import com.maidc.data.entity.rdr.RdrSelectionResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RDR队列筛选结果Repository
 */
@Repository
public interface RdrSelectionResultRepository extends
        JpaRepository<RdrSelectionResultEntity, Long>,
        JpaSpecificationExecutor<RdrSelectionResultEntity> {

    Optional<RdrSelectionResultEntity> findByResultCodeAndIsDeletedFalse(String resultCode);

    List<RdrSelectionResultEntity> findBySelectionIdAndIsDeletedFalseOrderByExecutionTimeDesc(Long selectionId);

    List<RdrSelectionResultEntity> findByCohortIdAndIsDeletedFalseOrderByExecutionTimeDesc(Long cohortId);

    List<RdrSelectionResultEntity> findByStatusAndIsDeletedFalse(String status);

    boolean existsByResultCodeAndIsDeletedFalse(String resultCode);

    @Query("SELECT r FROM RdrSelectionResultEntity r WHERE r.cohortId = ?1 " +
           "AND r.status = 'COMPLETED' AND r.isDeleted = false ORDER BY r.executionTime DESC LIMIT 1")
    Optional<RdrSelectionResultEntity> findLatestCompletedResult(Long cohortId);

    @Query("SELECT AVG(r.finalSelectedCount) FROM RdrSelectionResultEntity r WHERE r.status = 'COMPLETED'")
    Double getAverageSelectedCount();

    @Query("SELECT r FROM RdrSelectionResultEntity r WHERE r.status = 'RUNNING' AND r.isDeleted = false")
    List<RdrSelectionResultEntity> findRunningResults();

    Optional<RdrSelectionResultEntity> findByIdAndIsDeletedFalse(Long id);
}