package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.PermissibleValueEntity;
import com.maidc.data.entity.ValueDomainEntity;
import com.maidc.data.service.ValueDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import com.maidc.common.security.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/value-domains")
@RequiredArgsConstructor
public class ValueDomainController {

    private final ValueDomainService service;

    @RequirePermission("masterdata:read")
    @GetMapping
    public R<Page<ValueDomainEntity>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long conceptDomainId,
            @RequestParam(required = false) String domainType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(keyword, conceptDomainId, domainType, page, pageSize));
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/{id}")
    public R<ValueDomainEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/code/{code}")
    public R<ValueDomainEntity> getByCode(@PathVariable String code) {
        return R.ok(service.getByCode(code));
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/{id}/permissible-values")
    public R<List<PermissibleValueEntity>> listPermissibleValues(@PathVariable Long id) {
        return R.ok(service.listPermissibleValues(id));
    }

    @RequirePermission("masterdata:create")
    @PostMapping
    public R<ValueDomainEntity> create(@RequestBody ValueDomainEntity entity) {
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            return R.fail(400, "值域代码不能为空");
        }
        if (entity.getName() == null || entity.getName().isBlank()) {
            return R.fail(400, "值域名称不能为空");
        }
        if (entity.getDataType() == null || entity.getDataType().isBlank()) {
            return R.fail(400, "数据类型不能为空");
        }
        if (entity.getDomainType() == null || entity.getDomainType().isBlank()) {
            entity.setDomainType("ENUMERABLE");
        }
        return R.ok(service.create(entity));
    }

    @RequirePermission("masterdata:update")
    @PutMapping("/{id}")
    public R<ValueDomainEntity> update(@PathVariable Long id, @RequestBody ValueDomainEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @RequirePermission("masterdata:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    // ==================== 允许值管理 ====================

    @RequirePermission("masterdata:create")
    @PostMapping("/{id}/permissible-values")
    public R<PermissibleValueEntity> addPermissibleValue(@PathVariable Long id, @RequestBody PermissibleValueEntity pv) {
        if (pv.getValue() == null || pv.getValue().isBlank()) {
            return R.fail(400, "允许值不能为空");
        }
        return R.ok(service.addPermissibleValue(id, pv));
    }

    @RequirePermission("masterdata:create")
    @PostMapping("/{id}/permissible-values/batch")
    public R<List<PermissibleValueEntity>> batchImportPermissibleValues(
            @PathVariable Long id,
            @RequestBody List<PermissibleValueEntity> values) {
        return R.ok(service.batchImportPermissibleValues(id, values));
    }

    @RequirePermission("masterdata:update")
    @PutMapping("/permissible-values/{pvId}")
    public R<PermissibleValueEntity> updatePermissibleValue(@PathVariable Long pvId, @RequestBody PermissibleValueEntity pv) {
        return R.ok(service.updatePermissibleValue(pvId, pv));
    }

    @RequirePermission("masterdata:delete")
    @DeleteMapping("/permissible-values/{pvId}")
    public R<Void> deletePermissibleValue(@PathVariable Long pvId) {
        service.deletePermissibleValue(pvId);
        return R.ok();
    }
}