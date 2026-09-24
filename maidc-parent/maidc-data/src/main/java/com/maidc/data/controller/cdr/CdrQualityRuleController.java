package com.maidc.data.controller.cdr;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.cdr.CdrQualityRuleEntity;
import com.maidc.data.service.cdr.CdrQualityRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CDR数据质量规则Controller
 */
@RestController
@RequestMapping("/api/v1/cdr/quality-rules")
@RequiredArgsConstructor
public class CdrQualityRuleController {

    private final CdrQualityRuleService service;

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping
    public R<Page<CdrQualityRuleEntity>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String targetSchema,
            @RequestParam(required = false) String targetTable,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(category, targetSchema, targetTable, enabled, page, pageSize));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}")
    public R<CdrQualityRuleEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/code/{ruleCode}")
    public R<CdrQualityRuleEntity> getByCode(@PathVariable String ruleCode) {
        return R.ok(service.getByCode(ruleCode));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping
    public R<CdrQualityRuleEntity> create(@RequestBody CdrQualityRuleEntity entity) {
        return R.ok(service.create(entity));
    }

    @PreAuthorize("hasPermission('cdr:update')")
    @PutMapping("/{id}")
    public R<CdrQualityRuleEntity> update(@PathVariable Long id, @RequestBody CdrQualityRuleEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @PreAuthorize("hasPermission('cdr:delete')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @PreAuthorize("hasPermission('cdr:update')")
    @PostMapping("/{id}/toggle")
    public R<CdrQualityRuleEntity> toggleEnabled(@PathVariable Long id, @RequestParam Boolean enabled) {
        return R.ok(service.toggleEnabled(id, enabled));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/table/{schema}/{table}")
    public R<List<CdrQualityRuleEntity>> getRulesForTable(@PathVariable String schema, @PathVariable String table) {
        return R.ok(service.getEnabledRulesForTable(schema, table));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/categories")
    public R<List<String>> getCategories() {
        return R.ok(service.getCategories());
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        return R.ok(service.getStats());
    }
}