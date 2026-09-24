package com.maidc.data.controller.cdr;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.cdr.CdrQuarantineDataEntity;
import com.maidc.data.service.cdr.CdrQuarantineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CDR数据隔离区Controller
 */
@RestController
@RequestMapping("/api/v1/cdr/quarantine")
@RequiredArgsConstructor
public class CdrQuarantineController {

    private final CdrQuarantineService service;

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping
    public R<Page<CdrQuarantineDataEntity>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String sourceTable,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(status, priority, sourceTable, assignedTo, page, pageSize));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}")
    public R<CdrQuarantineDataEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/code/{quarantineCode}")
    public R<CdrQuarantineDataEntity> getByCode(@PathVariable String quarantineCode) {
        return R.ok(service.getByCode(quarantineCode));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/priority/{priority}")
    public R<List<CdrQuarantineDataEntity>> getByPriority(@PathVariable String priority) {
        return R.ok(service.getPendingByPriority(priority));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/my-assignments")
    public R<List<CdrQuarantineDataEntity>> getMyAssignments(@RequestParam String assignedTo) {
        return R.ok(service.getMyAssignments(assignedTo));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping
    public R<CdrQuarantineDataEntity> create(@RequestBody CdrQuarantineDataEntity entity) {
        return R.ok(service.create(entity));
    }

    @PreAuthorize("hasPermission('cdr:update')")
    @PostMapping("/{id}/assign")
    public R<CdrQuarantineDataEntity> assign(@PathVariable Long id, @RequestParam String assignedTo) {
        return R.ok(service.assign(id, assignedTo));
    }

    @PreAuthorize("hasPermission('cdr:update')")
    @PostMapping("/{id}/process")
    public R<CdrQuarantineDataEntity> process(
            @PathVariable Long id,
            @RequestParam String action,
            @RequestParam String processedBy,
            @RequestParam(required = false) String fixedData,
            @RequestParam(required = false) String notes) {
        return R.ok(service.process(id, action, processedBy, fixedData, notes));
    }

    @PreAuthorize("hasPermission('cdr:update')")
    @PostMapping("/batch-assign")
    public R<Void> batchAssign(@RequestBody List<Long> ids, @RequestParam String assignedTo) {
        service.batchAssign(ids, assignedTo);
        return R.ok();
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        return R.ok(service.getStats());
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/source/{schema}/{table}")
    public R<List<CdrQuarantineDataEntity>> getBySourceTable(@PathVariable String schema, @PathVariable String table) {
        return R.ok(service.getBySourceTable(schema, table));
    }
}