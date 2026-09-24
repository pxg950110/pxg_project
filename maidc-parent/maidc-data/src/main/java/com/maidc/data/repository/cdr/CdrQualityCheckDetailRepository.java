package com.maidc.data.repository.cdr;

import com.maidc.data.entity.cdr.CdrQualityCheckDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CDR数据质量检测明细Repository
 */
@Repository
public interface CdrQualityCheckDetailRepository extends
        JpaRepository<CdrQualityCheckDetailEntity, Long>,
        JpaSpecificationExecutor<CdrQualityCheckDetailEntity> {

    List<CdrQualityCheckDetailEntity> findByBatchIdAndIsDeletedFalse(Long batchId);

    List<CdrQualityCheckDetailEntity> findByBatchIdAndCheckResultAndIsDeletedFalse(Long batchId, String result);

    List<CdrQualityCheckDetailEntity> findByBatchIdAndErrorTypeAndIsDeletedFalse(Long batchId, String errorType);

    List<CdrQualityCheckDetailEntity> findByRuleIdAndIsDeletedFalse(Long ruleId);

    List<CdrQualityCheckDetailEntity> findByIsQuarantinedAndIsDeletedFalse(Boolean isQuarantined);

    @Query("SELECT d.errorType, COUNT(d) FROM CdrQualityCheckDetailEntity d " +
           "WHERE d.batchId = ?1 AND d.checkResult = 'FAIL' AND d.isDeleted = false " +
           "GROUP BY d.errorType")
    List<Object[]> countErrorsByType(Long batchId);

    @Query("SELECT d.targetColumn, COUNT(d) FROM CdrQualityCheckDetailEntity d " +
           "WHERE d.batchId = ?1 AND d.checkResult = 'FAIL' AND d.isDeleted = false " +
           "GROUP BY d.targetColumn")
    List<Object[]> countErrorsByColumn(Long batchId);

    @Query("SELECT COUNT(d) FROM CdrQualityCheckDetailEntity d " +
           "WHERE d.batchId = ?1 AND d.checkResult = 'PASS' AND d.isDeleted = false")
    long countPassedByBatch(Long batchId);

    @Query("SELECT COUNT(d) FROM CdrQualityCheckDetailEntity d " +
           "WHERE d.batchId = ?1 AND d.checkResult = 'FAIL' AND d.isDeleted = false")
    long countFailedByBatch(Long batchId);
}