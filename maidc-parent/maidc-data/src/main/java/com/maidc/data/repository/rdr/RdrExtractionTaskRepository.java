package com.maidc.data.repository.rdr;

import com.maidc.data.entity.rdr.RdrExtractionTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RDR数据抽取任务Repository
 */
@Repository
public interface RdrExtractionTaskRepository extends
        JpaRepository<RdrExtractionTaskEntity, Long>,
        JpaSpecificationExecutor<RdrExtractionTaskEntity> {

    Optional<RdrExtractionTaskEntity> findByTaskCodeAndIsDeletedFalse(String taskCode);

    List<RdrExtractionTaskEntity> findByProjectIdAndIsDeletedFalse(Long projectId);

    List<RdrExtractionTaskEntity> findByStatusAndIsDeletedFalse(String status);

    boolean existsByTaskCodeAndIsDeletedFalse(String taskCode);

    @Modifying
    @Query("UPDATE RdrExtractionTaskEntity t SET t.executionCount = t.executionCount + 1, " +
           "t.lastExecutionAt = CURRENT_TIMESTAMP, t.lastExecutionStatus = ?2 WHERE t.id = ?1")
    void updateExecutionStats(Long taskId, String lastExecutionStatus);

    @Query("SELECT COUNT(t) FROM RdrExtractionTaskEntity t WHERE t.status = 'ACTIVE'")
    long countActiveTasks();

    @Query("SELECT t FROM RdrExtractionTaskEntity t WHERE t.scheduleType = 'CRON' " +
           "AND t.status = 'ACTIVE' AND t.isDeleted = false")
    List<RdrExtractionTaskEntity> findScheduledActiveTasks();

    @Query("SELECT DISTINCT t.extractionType FROM RdrExtractionTaskEntity t WHERE t.isDeleted = false")
    List<String> findDistinctExtractionTypes();

    Optional<RdrExtractionTaskEntity> findByIdAndIsDeletedFalse(Long id);
}