package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.LocalConceptEntity;
import com.maidc.data.service.LocalConceptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import com.maidc.common.security.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/masterdata/local-concepts")
@RequiredArgsConstructor
public class LocalConceptController {

    private final LocalConceptService service;

    @RequirePermission("masterdata:read")
    @GetMapping
    public R<Page<LocalConceptEntity>> list(
            @RequestParam Long institutionId,
            @RequestParam(required = false) Long codeSystemId,
            @RequestParam(required = false) String mappingStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size,
            @RequestParam(required = false, defaultValue = "20") int size) {
        int effectiveSize = size != 20 ? size : page_size;
        return R.ok(service.list(institutionId, codeSystemId, mappingStatus, page, effectiveSize));
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/unmapped")
    public R<Page<LocalConceptEntity>> unmapped(
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) Long codeSystemId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        return R.ok(service.getUnmapped(institutionId, codeSystemId, page, page_size));
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/translate")
    public R<Map<String, Object>> translate(
            @RequestParam Long institutionId,
            @RequestParam Long codeSystemId,
            @RequestParam String localCode) {
        return R.ok(service.translateById(institutionId, codeSystemId, localCode));
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats(
            @RequestParam Long institutionId,
            @RequestParam Long codeSystemId) {
        return R.ok(service.getStats(institutionId, codeSystemId));
    }

    @RequirePermission("masterdata:create")
    @PostMapping
    public R<LocalConceptEntity> create(@RequestBody LocalConceptEntity entity) {
        if (entity.getInstitutionId() == null) {
            return R.fail(400, "机构ID不能为空");
        }
        if (entity.getCodeSystemId() == null) {
            return R.fail(400, "编码体系ID不能为空");
        }
        if (entity.getLocalCode() == null || entity.getLocalCode().isBlank()) {
            return R.fail(400, "本地编码不能为空");
        }
        if (entity.getLocalName() == null || entity.getLocalName().isBlank()) {
            return R.fail(400, "本地编码名称不能为空");
        }
        return R.ok(service.create(entity));
    }

    @RequirePermission("masterdata:create")
    @PostMapping("/batch")
    public R<List<LocalConceptEntity>> batch(@RequestBody List<LocalConceptEntity> entities) {
        return R.ok(service.batchCreate(entities));
    }

    @RequirePermission("masterdata:update")
    @PutMapping("/{id}")
    public R<LocalConceptEntity> update(@PathVariable Long id, @RequestBody LocalConceptEntity entity) {
        return R.ok(service.update(id, entity));
    }
}
