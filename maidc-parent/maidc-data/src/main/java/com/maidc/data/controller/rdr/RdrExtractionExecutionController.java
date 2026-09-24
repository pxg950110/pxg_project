package com.maidc.data.controller.rdr;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.rdr.RdrExtractionExecutionEntity;
import com.maidc.data.service.rdr.RdrExtractionExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RDR抽取任务执行Controller
 */
@RestController
@RequestMapping("/api/v1/rdr/extraction-executions")
@RequiredArgsConstructor
public class RdrExtractionExecutionController {

    private final RdrExtractionExecutionService service;

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping
    public R<Page<RdrExtractionExecutionEntity>> list(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String executionType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(taskId, status, executionType, page, pageSize));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/{id}")
    public R<RdrExtractionExecutionEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/code/{executionCode}")
    public R<RdrExtractionExecutionEntity> getByCode(@PathVariable String executionCode) {
        return R.ok(service.getByCode(executionCode));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/task/{taskId}")
    public R<List<RdrExtractionExecutionEntity>> getByTask(@PathVariable Long taskId) {
        return R.ok(service.getByTask(taskId));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/task/{taskId}/latest-success")
    public R<RdrExtractionExecutionEntity> getLatestSuccess(@PathVariable Long taskId) {
        RdrExtractionExecutionEntity exec = service.getLatestSuccess(taskId);
        return exec != null ? R.ok(exec) : R.ok(null);
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/running")
    public R<List<RdrExtractionExecutionEntity>> getRunningExecutions() {
        return R.ok(service.getRunningExecutions());
    }

    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/start")
    public R<RdrExtractionExecutionEntity> startExecution(
            @RequestParam Long taskId,
            @RequestParam(required = false) String executionType,
            @RequestParam(required = false) String triggeredBy) {
        return R.ok(service.startExecution(taskId, executionType, triggeredBy));
    }

    @PreAuthorize("hasPermission('rdr:update')")
    @PostMapping("/{id}/cancel")
    public R<Void> cancelExecution(@PathVariable Long id) {
        service.cancel(id);
        return R.ok();
    }

    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/{id}/retry")
    public R<RdrExtractionExecutionEntity> retryExecution(
            @PathVariable Long id,
            @RequestParam(required = false) String triggeredBy) {
        return R.ok(service.retry(id, triggeredBy));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        return R.ok(service.getStats());
    }
}