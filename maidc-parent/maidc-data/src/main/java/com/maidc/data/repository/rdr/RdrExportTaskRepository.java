package com.maidc.data.repository.rdr;

import com.maidc.data.entity.rdr.RdrExportTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RDR数据导出任务Repository
 */
@Repository
public interface RdrExportTaskRepository extends
        JpaRepository<RdrExportTaskEntity, Long>,
        JpaSpecificationExecutor<RdrExportTaskEntity> {

    Optional<RdrExportTaskEntity> findByTaskCodeAndIsDeletedFalse(String taskCode);

    List<RdrExportTaskEntity> findByDatasetVersionIdAndIsDeletedFalseOrderByCreatedAtDesc(Long datasetVersionId);

    List<RdrExportTaskEntity> findByStatusAndIsDeletedFalse(String status);

    List<RdrExportTaskEntity> findByRequestedByAndIsDeletedFalseOrderByCreatedAtDesc(String requestedBy);

    boolean existsByTaskCodeAndIsDeletedFalse(String taskCode);

    @Modifying
    @Query("UPDATE RdrExportTaskEntity e SET e.downloadCount = e.downloadCount + 1 WHERE e.id = ?1")
    void incrementDownloadCount(Long taskId);

    @Query("SELECT COUNT(e) FROM RdrExportTaskEntity e WHERE e.status = 'PENDING'")
    long countPendingExports();

    @Query("SELECT COUNT(e) FROM RdrExportTaskEntity e WHERE e.status = 'SUCCESS'")
    long countSuccessExports();

    @Query("SELECT e FROM RdrExportTaskEntity e WHERE e.status = 'RUNNING' AND e.isDeleted = false")
    List<RdrExportTaskEntity> findRunningExports();
}