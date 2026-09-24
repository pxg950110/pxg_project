package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.PropertyEntity;
import com.maidc.data.service.PropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import com.maidc.common.security.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService service;

    @RequirePermission("masterdata:read")
    @GetMapping
    public R<Page<PropertyEntity>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(keyword, page, pageSize));
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/all")
    public R<List<PropertyEntity>> listAll() {
        return R.ok(service.listAll());
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/{id}")
    public R<PropertyEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/code/{code}")
    public R<PropertyEntity> getByCode(@PathVariable String code) {
        return R.ok(service.getByCode(code));
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/{id}/children")
    public R<List<PropertyEntity>> listChildren(@PathVariable Long id) {
        return R.ok(service.listByParent(id));
    }

    @RequirePermission("masterdata:create")
    @PostMapping
    public R<PropertyEntity> create(@RequestBody PropertyEntity entity) {
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            return R.fail(400, "特性代码不能为空");
        }
        if (entity.getName() == null || entity.getName().isBlank()) {
            return R.fail(400, "特性名称不能为空");
        }
        return R.ok(service.create(entity));
    }

    @RequirePermission("masterdata:update")
    @PutMapping("/{id}")
    public R<PropertyEntity> update(@PathVariable Long id, @RequestBody PropertyEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @RequirePermission("masterdata:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}