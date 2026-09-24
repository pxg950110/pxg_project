package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.DataElementConceptEntity;
import com.maidc.data.service.DataElementConceptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import com.maidc.common.security.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/masterdata/data-element-concepts")
@RequiredArgsConstructor
public class DataElementConceptController {

    private final DataElementConceptService service;

    @RequirePermission("masterdata:read")
    @GetMapping
    public R<Page<DataElementConceptEntity>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String objectClassCode,
            @RequestParam(required = false) String propertyCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(keyword, objectClassCode, propertyCode, page, pageSize));
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/{id}")
    public R<DataElementConceptEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/code/{code}")
    public R<DataElementConceptEntity> getByCode(@PathVariable String code) {
        return R.ok(service.getByCode(code));
    }

    @RequirePermission("masterdata:create")
    @PostMapping
    public R<DataElementConceptEntity> create(@RequestBody DataElementConceptEntity entity) {
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            return R.fail(400, "数据元概念代码不能为空");
        }
        if (entity.getName() == null || entity.getName().isBlank()) {
            return R.fail(400, "数据元概念名称不能为空");
        }
        return R.ok(service.create(entity));
    }

    @RequirePermission("masterdata:update")
    @PutMapping("/{id}")
    public R<DataElementConceptEntity> update(@PathVariable Long id, @RequestBody DataElementConceptEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @RequirePermission("masterdata:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}