package com.maidc.data.controller.cdr;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.cdr.CdrQualityCheckBatchEntity;
import com.maidc.data.entity.cdr.CdrQualityCheckDetailEntity;
import com.maidc.data.service.cdr.CdrQualityCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CDR数据质量检测Controller
 */
@RestController
@RequestMapping("/api/v1/cdr/quality-checks")
@RequiredArgsConstructor
public class CdrQualityCheckController {

    private final CdrQualityCheckService service;

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/batches")
    public R<Page<CdrQualityCheckBatchEntity>> listBatches(
            @RequestParam(required = false) String targetSchema,
            @RequestParam(required = false) String targetTable,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.listBatch(targetSchema, targetTable, status, page, pageSize));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/batches/{id}")
    public R<CdrQualityCheckBatchEntity> getBatch(@PathVariable Long id) {
        return R.ok(service.getBatchById(id));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/batches/{batchId}/details")
    public R<List<CdrQualityCheckDetailEntity>> getDetails(@PathVariable Long batchId) {
        return R.ok(service.getDetailsByBatch(batchId));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/batches/{batchId}/failed-details")
    public R<List<CdrQualityCheckDetailEntity>> getFailedDetails(@PathVariable Long batchId) {
        return R.ok(service.getFailedDetailsByBatch(batchId));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/batches/{batchId}/error-stats")
    public R<Map<String, Object>> getErrorStats(@PathVariable Long batchId) {
        return R.ok(service.getErrorStatsByBatch(batchId));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/batches/start")
    public R<CdrQualityCheckBatchEntity> startBatch(
            @RequestParam String targetSchema,
            @RequestParam String targetTable,
            @RequestParam(required = false) String checkType,
            @RequestParam(required = false) String triggeredBy) {
        return R.ok(service.startBatch(targetSchema, targetTable, checkType, triggeredBy));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/batches/recent")
    public R<List<CdrQualityCheckBatchEntity>> getRecentBatches(@RequestParam(defaultValue = "10") int limit) {
        return R.ok(service.getRecentBatches(limit));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/batches/average-pass-rate")
    public R<Double> getAveragePassRate() {
        return R.ok(service.getAveragePassRate());
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/batches/pass-rate-by-table")
    public R<List<Object[]>> getPassRateByTable() {
        return R.ok(service.getPassRateByTable());
    }
}