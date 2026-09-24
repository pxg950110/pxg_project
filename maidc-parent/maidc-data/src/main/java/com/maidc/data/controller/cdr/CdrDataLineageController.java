package com.maidc.data.controller.cdr;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.cdr.CdrDataLineageEntity;
import com.maidc.data.service.cdr.CdrDataLineageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CDR数据血缘Controller
 */
@RestController
@RequestMapping("/api/v1/cdr/lineage")
@RequiredArgsConstructor
public class CdrDataLineageController {

    private final CdrDataLineageService service;

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping
    public R<Page<CdrDataLineageEntity>> list(
            @RequestParam(required = false) String targetSchema,
            @RequestParam(required = false) String targetTable,
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(targetSchema, targetTable, sourceSystem, page, pageSize));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}")
    public R<CdrDataLineageEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/target/{schema}/{table}/{column}")
    public R<List<CdrDataLineageEntity>> getByTargetColumn(
            @PathVariable String schema,
            @PathVariable String table,
            @PathVariable String column) {
        return R.ok(service.getByTargetColumn(schema, table, column));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/source/{system}")
    public R<List<CdrDataLineageEntity>> getBySourceSystem(@PathVariable String system) {
        return R.ok(service.getBySourceSystem(system));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/etl-job/{etlJobId}")
    public R<List<CdrDataLineageEntity>> getByEtlJob(@PathVariable Long etlJobId) {
        return R.ok(service.getByEtlJob(etlJobId));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping
    public R<CdrDataLineageEntity> create(@RequestBody CdrDataLineageEntity entity) {
        return R.ok(service.create(entity));
    }

    @PreAuthorize("hasPermission('cdr:update')")
    @PutMapping("/{id}")
    public R<CdrDataLineageEntity> update(@PathVariable Long id, @RequestBody CdrDataLineageEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/trace/{schema}/{table}/{column}")
    public R<List<Map<String, Object>>> traceBack(
            @PathVariable String schema,
            @PathVariable String table,
            @PathVariable String column) {
        return R.ok(service.traceBack(schema, table, column));
    }

    @PreAuthorize("hasPermission('cdr:update')")
    @PostMapping("/{id}/deactivate")
    public R<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return R.ok();
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        return R.ok(service.getStats());
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/source-systems")
    public R<List<String>> getSourceSystems() {
        return R.ok(service.getSourceSystems());
    }
}