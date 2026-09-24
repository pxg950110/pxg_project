package com.maidc.data.repository.cdr;

import com.maidc.data.entity.cdr.CdrQualityCheckBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CDR数据质量检测批次Repository
 */
@Repository
public interface CdrQualityCheckBatchRepository extends
        JpaRepository<CdrQualityCheckBatchEntity, Long>,
        JpaSpecificationExecutor<CdrQualityCheckBatchEntity> {

    Optional<CdrQualityCheckBatchEntity> findByBatchCodeAndIsDeletedFalse(String batchCode);

    List<CdrQualityCheckBatchEntity> findByTargetSchemaAndTargetTableAndIsDeletedFalse(String schema, String table);

    List<CdrQualityCheckBatchEntity> findByStatusAndIsDeletedFalse(String status);

    @Query("SELECT b FROM CdrQualityCheckBatchEntity b WHERE b.isDeleted = false " +
           "ORDER BY b.checkTime DESC LIMIT ?1")
    List<CdrQualityCheckBatchEntity> findRecentBatches(int limit);

    @Query("SELECT AVG(b.passRate) FROM CdrQualityCheckBatchEntity b " +
           "WHERE b.status = 'COMPLETED' AND b.isDeleted = false")
    Double getAveragePassRate();

    @Query("SELECT b.targetTable, AVG(b.passRate) FROM CdrQualityCheckBatchEntity b " +
           "WHERE b.status = 'COMPLETED' AND b.isDeleted = false GROUP BY b.targetTable")
    List<Object[]> getPassRateByTable();

    @Query("SELECT COUNT(b) FROM CdrQualityCheckBatchEntity b WHERE b.status = 'RUNNING'")
    long countRunningBatches();

    Optional<CdrQualityCheckBatchEntity> findByIdAndIsDeletedFalse(Long id);
}