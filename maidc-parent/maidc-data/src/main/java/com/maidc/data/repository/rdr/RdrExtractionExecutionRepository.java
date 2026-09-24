package com.maidc.data.repository.rdr;

import com.maidc.data.entity.rdr.RdrExtractionExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RDR抽取任务执行记录Repository
 */
@Repository
public interface RdrExtractionExecutionRepository extends
        JpaRepository<RdrExtractionExecutionEntity, Long>,
        JpaSpecificationExecutor<RdrExtractionExecutionEntity> {

    Optional<RdrExtractionExecutionEntity> findByExecutionCodeAndIsDeletedFalse(String executionCode);

    List<RdrExtractionExecutionEntity> findByTaskIdAndIsDeletedFalseOrderByStartedAtDesc(Long taskId);

    List<RdrExtractionExecutionEntity> findByStatusAndIsDeletedFalse(String status);

    boolean existsByExecutionCodeAndIsDeletedFalse(String executionCode);

    @Query("SELECT e FROM RdrExtractionExecutionEntity e WHERE e.status = 'RUNNING' AND e.isDeleted = false")
    List<RdrExtractionExecutionEntity> findRunningExecutions();

    @Query("SELECT e FROM RdrExtractionExecutionEntity e WHERE e.taskId = ?1 " +
           "AND e.status = 'SUCCESS' AND e.isDeleted = false ORDER BY e.finishedAt DESC LIMIT 1")
    Optional<RdrExtractionExecutionEntity> findLatestSuccessExecution(Long taskId);

    @Query("SELECT COUNT(e) FROM RdrExtractionExecutionEntity e WHERE e.status = 'SUCCESS'")
    long countSuccessExecutions();

    @Query("SELECT COUNT(e) FROM RdrExtractionExecutionEntity e WHERE e.status = 'FAILED'")
    long countFailedExecutions();

    @Query("SELECT AVG(e.durationMs) FROM RdrExtractionExecutionEntity e WHERE e.status = 'SUCCESS'")
    Double getAverageDuration();

    Optional<RdrExtractionExecutionEntity> findByIdAndIsDeletedFalse(Long id);
}