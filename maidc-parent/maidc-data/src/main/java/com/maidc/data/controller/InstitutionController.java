package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.InstitutionEntity;
import com.maidc.data.service.InstitutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/institutions")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService service;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping
    public R<List<InstitutionEntity>> list() {
        return R.ok(service.list());
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping
    public R<InstitutionEntity> create(@RequestBody InstitutionEntity entity) {
        if (entity.getInstCode() == null || entity.getInstCode().isBlank()) {
            return R.fail(400, "机构编码不能为空");
        }
        if (entity.getName() == null || entity.getName().isBlank()) {
            return R.fail(400, "机构名称不能为空");
        }
        return R.ok(service.create(entity));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PutMapping("/{id}")
    public R<InstitutionEntity> update(@PathVariable Long id, @RequestBody InstitutionEntity entity) {
        return R.ok(service.update(id, entity));
    }
}
