package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.ConceptEntity;
import com.maidc.data.service.ConceptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/masterdata/concepts")
@RequiredArgsConstructor
public class ConceptController {

    private final ConceptService service;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping
    public R<Page<ConceptEntity>> list(
            @RequestParam(required = false) Long codeSystemId,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(codeSystemId, domain, keyword, page, pageSize));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}")
    public R<ConceptEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/search")
    public R<Page<ConceptEntity>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) Long codeSystemId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.search(keyword, codeSystemId, page, pageSize));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping
    public R<ConceptEntity> create(@RequestBody ConceptEntity entity) {
        if (entity.getConceptCode() == null || entity.getConceptCode().isBlank()) {
            return R.fail(400, "概念编码不能为空");
        }
        if (entity.getCodeSystemId() == null) {
            return R.fail(400, "编码体系ID不能为空");
        }
        if (entity.getName() == null || entity.getName().isBlank()) {
            return R.fail(400, "概念名称不能为空");
        }
        return R.ok(service.create(entity));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PutMapping("/{id}")
    public R<ConceptEntity> update(@PathVariable Long id, @RequestBody ConceptEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}
