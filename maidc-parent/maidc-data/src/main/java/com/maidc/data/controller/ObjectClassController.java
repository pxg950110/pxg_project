package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.ObjectClassEntity;
import com.maidc.data.service.ObjectClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import com.maidc.common.security.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/object-classes")
@RequiredArgsConstructor
public class ObjectClassController {

    private final ObjectClassService service;

    @RequirePermission("masterdata:read")
    @GetMapping
    public R<Page<ObjectClassEntity>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(keyword, page, pageSize));
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/all")
    public R<List<ObjectClassEntity>> listAll() {
        return R.ok(service.listAll());
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/{id}")
    public R<ObjectClassEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/code/{code}")
    public R<ObjectClassEntity> getByCode(@PathVariable String code) {
        return R.ok(service.getByCode(code));
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/{id}/children")
    public R<List<ObjectClassEntity>> listChildren(@PathVariable Long id) {
        return R.ok(service.listByParent(id));
    }

    @RequirePermission("masterdata:create")
    @PostMapping
    public R<ObjectClassEntity> create(@RequestBody ObjectClassEntity entity) {
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            return R.fail(400, "对象类代码不能为空");
        }
        if (entity.getName() == null || entity.getName().isBlank()) {
            return R.fail(400, "对象类名称不能为空");
        }
        return R.ok(service.create(entity));
    }

    @RequirePermission("masterdata:update")
    @PutMapping("/{id}")
    public R<ObjectClassEntity> update(@PathVariable Long id, @RequestBody ObjectClassEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @RequirePermission("masterdata:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}