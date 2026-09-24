package com.maidc.data.service.cdr;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.cdr.CdrQualityCheckBatchEntity;
import com.maidc.data.entity.cdr.CdrQualityCheckDetailEntity;
import com.maidc.data.repository.cdr.CdrQualityCheckBatchRepository;
import com.maidc.data.repository.cdr.CdrQualityCheckDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CDR数据质量检测服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdrQualityCheckService {

    private final CdrQualityCheckBatchRepository batchRepository;
    private final CdrQualityCheckDetailRepository detailRepository;

    public Page<CdrQualityCheckBatchEntity> listBatch(String targetSchema, String targetTable,
                                                       String status, int page, int size) {
        Specification<CdrQualityCheckBatchEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (targetSchema != null && !targetSchema.isBlank()) {
                predicates.add(cb.equal(root.get("targetSchema"), targetSchema));
            }
            if (targetTable != null && !targetTable.isBlank()) {
                predicates.add(cb.equal(root.get("targetTable"), targetTable));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return batchRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "checkTime")));
    }

    public CdrQualityCheckBatchEntity getBatchById(Long id) {
        return batchRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "检测批次不存在: " + id));
    }

    public List<CdrQualityCheckDetailEntity> getDetailsByBatch(Long batchId) {
        return detailRepository.findByBatchIdAndIsDeletedFalse(batchId);
    }

    public List<CdrQualityCheckDetailEntity> getFailedDetailsByBatch(Long batchId) {
        return detailRepository.findByBatchIdAndCheckResultAndIsDeletedFalse(batchId, "FAIL");
    }

    @Transactional
    public CdrQualityCheckBatchEntity startBatch(String targetSchema, String targetTable,
                                                  String checkType, String triggeredBy) {
        CdrQualityCheckBatchEntity batch = new CdrQualityCheckBatchEntity();
        batch.setBatchCode("QC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        batch.setBatchName(targetTable + "质量检测-" + LocalDateTime.now().toString());
        batch.setTargetSchema(targetSchema);
        batch.setTargetTable(targetTable);
        batch.setCheckType(checkType != null ? checkType : "FULL");
        batch.setCheckTime(LocalDateTime.now());
        batch.setTotalRecords(0L);
        batch.setPassedRecords(0L);
        batch.setFailedRecords(0L);
        batch.setQuarantinedRecords(0L);
        batch.setPassRate(BigDecimal.ZERO);
        batch.setStatus("RUNNING");
        batch.setStartedAt(LocalDateTime.now());
        batch.setTriggeredBy(triggeredBy != null ? triggeredBy : "MANUAL");
        batch.setOrgId(0L);
        return batchRepository.save(batch);
    }

    @Transactional
    public void updateBatchProgress(Long batchId, Long totalRecords, Long passedRecords, Long failedRecords) {
        CdrQualityCheckBatchEntity batch = getBatchById(batchId);
        batch.setTotalRecords(totalRecords);
        batch.setPassedRecords(passedRecords);
        batch.setFailedRecords(failedRecords);
        if (totalRecords > 0) {
            BigDecimal passRate = BigDecimal.valueOf(passedRecords)
                    .divide(BigDecimal.valueOf(totalRecords), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            batch.setPassRate(passRate);
        }
        batchRepository.save(batch);
    }

    @Transactional
    public void completeBatch(Long batchId, Long quarantinedRecords) {
        CdrQualityCheckBatchEntity batch = getBatchById(batchId);
        batch.setQuarantinedRecords(quarantinedRecords);
        batch.setStatus("COMPLETED");
        batch.setFinishedAt(LocalDateTime.now());
        batch.setDurationMs(
            batch.getStartedAt() != null && batch.getFinishedAt() != null ?
            (batch.getFinishedAt().toEpochSecond(java.time.ZoneOffset.UTC) -
             batch.getStartedAt().toEpochSecond(java.time.ZoneOffset.UTC)) * 1000 : null
        );
        batchRepository.save(batch);
        log.info("检测批次完成: id={}, passRate={}", batchId, batch.getPassRate());
    }

    @Transactional
    public void failBatch(Long batchId, String errorMessage) {
        CdrQualityCheckBatchEntity batch = getBatchById(batchId);
        batch.setStatus("FAILED");
        batch.setFinishedAt(LocalDateTime.now());
        batch.setErrorSummary("{\"error\":\"" + errorMessage + "\"}");
        batchRepository.save(batch);
        log.error("检测批次失败: id={}, error={}", batchId, errorMessage);
    }

    @Transactional
    public CdrQualityCheckDetailEntity addDetail(Long batchId, Long ruleId, String recordId,
                                                   String targetColumn, String actualValue,
                                                   String expectedValue, String errorType,
                                                   String errorMessage, String severity) {
        CdrQualityCheckDetailEntity detail = new CdrQualityCheckDetailEntity();
        detail.setBatchId(batchId);
        detail.setRuleId(ruleId);
        detail.setRecordId(recordId);
        detail.setTargetColumn(targetColumn);
        detail.setActualValue(actualValue);
        detail.setExpectedValue(expectedValue);
        detail.setErrorType(errorType);
        detail.setErrorMessage(errorMessage);
        detail.setSeverity(severity);
        detail.setCheckResult("FAIL");
        detail.setIsQuarantined(false);
        detail.setIsFixed(false);
        detail.setOrgId(0L);
        return detailRepository.save(detail);
    }

    public Map<String, Object> getErrorStatsByBatch(Long batchId) {
        List<Object[]> byType = detailRepository.countErrorsByType(batchId);
        List<Object[]> byColumn = detailRepository.countErrorsByColumn(batchId);
        long passed = detailRepository.countPassedByBatch(batchId);
        long failed = detailRepository.countFailedByBatch(batchId);
        return Map.of(
            "passed", passed,
            "failed", failed,
            "byType", byType,
            "byColumn", byColumn
        );
    }

    public List<CdrQualityCheckBatchEntity> getRecentBatches(int limit) {
        return batchRepository.findRecentBatches(limit);
    }

    public Double getAveragePassRate() {
        return batchRepository.getAveragePassRate();
    }

    public List<Object[]> getPassRateByTable() {
        return batchRepository.getPassRateByTable();
    }
}