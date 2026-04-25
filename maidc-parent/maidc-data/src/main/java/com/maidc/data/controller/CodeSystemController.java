package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.CodeSystemEntity;
import com.maidc.data.service.CodeSystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/masterdata/code-systems")
@RequiredArgsConstructor
public class CodeSystemController {

    private final CodeSystemService service;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping
    public R<List<CodeSystemEntity>> list() {
        return R.ok(service.listAll());
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}")
    public R<CodeSystemEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/stats")
    public R<Map<String, Object>> stats(@PathVariable Long id) {
        return R.ok(service.getStats(id));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping
    public R<CodeSystemEntity> create(@RequestBody CodeSystemEntity entity) {
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            return R.fail(400, "编码体系代码不能为空");
        }
        if (entity.getName() == null || entity.getName().isBlank()) {
            return R.fail(400, "编码体系名称不能为空");
        }
        return R.ok(service.create(entity));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PutMapping("/{id}")
    public R<CodeSystemEntity> update(@PathVariable Long id, @RequestBody CodeSystemEntity entity) {
        return R.ok(service.update(id, entity));
    }
}
