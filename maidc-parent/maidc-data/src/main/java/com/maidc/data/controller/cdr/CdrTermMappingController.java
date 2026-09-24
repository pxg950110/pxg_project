package com.maidc.data.controller.cdr;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.cdr.CdrTermMappingEntity;
import com.maidc.data.service.cdr.CdrTermMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CDR术语映射Controller
 */
@RestController
@RequestMapping("/api/v1/cdr/term-mappings")
@RequiredArgsConstructor
public class CdrTermMappingController {

    private final CdrTermMappingService service;

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping
    public R<Page<CdrTermMappingEntity>> list(
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(required = false) String termType,
            @RequestParam(required = false) String standardSystem,
            @RequestParam(required = false) String mappingStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(sourceSystem, termType, standardSystem, mappingStatus, keyword, page, pageSize));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}")
    public R<CdrTermMappingEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/local-term")
    public R<List<CdrTermMappingEntity>> getByLocalTerm(
            @RequestParam String sourceSystem,
            @RequestParam String termType,
            @RequestParam String localCode) {
        return R.ok(service.getByLocalTerm(sourceSystem, termType, localCode));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/search")
    public R<List<CdrTermMappingEntity>> searchByLocalName(
            @RequestParam String localName,
            @RequestParam(required = false) String standardSystem) {
        return R.ok(service.searchByLocalName(localName, standardSystem));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/pending")
    public R<List<CdrTermMappingEntity>> getPendingMappings() {
        return R.ok(service.getPendingMappings());
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping
    public R<CdrTermMappingEntity> create(@RequestBody CdrTermMappingEntity entity) {
        return R.ok(service.create(entity));
    }

    @PreAuthorize("hasPermission('cdr:update')")
    @PutMapping("/{id}")
    public R<CdrTermMappingEntity> update(@PathVariable Long id, @RequestBody CdrTermMappingEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @PreAuthorize("hasPermission('cdr:update')")
    @PostMapping("/{id}/validate")
    public R<CdrTermMappingEntity> validate(@PathVariable Long id, @RequestParam String validatedBy) {
        return R.ok(service.validate(id, validatedBy));
    }

    @PreAuthorize("hasPermission('cdr:update')")
    @PostMapping("/{id}/confirm")
    public R<CdrTermMappingEntity> confirm(@PathVariable Long id, @RequestParam String validatedBy) {
        return R.ok(service.confirm(id, validatedBy));
    }

    @PreAuthorize("hasPermission('cdr:update')")
    @PostMapping("/{id}/reject")
    public R<CdrTermMappingEntity> reject(@PathVariable Long id, @RequestParam String validatedBy) {
        return R.ok(service.reject(id, validatedBy));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/best-match")
    public R<CdrTermMappingEntity> findBestMatch(
            @RequestParam String sourceSystem,
            @RequestParam String termType,
            @RequestParam String localCode) {
        CdrTermMappingEntity match = service.findBestMatch(sourceSystem, termType, localCode);
        return match != null ? R.ok(match) : R.ok(null);
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        return R.ok(service.getStats());
    }
}