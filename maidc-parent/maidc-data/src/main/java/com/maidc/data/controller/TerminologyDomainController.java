package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.TerminologyDomainEntity;
import com.maidc.data.service.TerminologyDomainService;
import lombok.RequiredArgsConstructor;
import com.maidc.common.security.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/domains")
@RequiredArgsConstructor
public class TerminologyDomainController {

    private final TerminologyDomainService service;

    @RequirePermission("masterdata:read")
    @GetMapping
    public R<List<TerminologyDomainEntity>> list() {
        return R.ok(service.listAll());
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/{id}")
    public R<TerminologyDomainEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @RequirePermission("masterdata:create")
    @PostMapping
    public R<TerminologyDomainEntity> create(@RequestBody TerminologyDomainEntity entity) {
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            return R.fail(400, "领域代码不能为空");
        }
        if (entity.getName() == null || entity.getName().isBlank()) {
            return R.fail(400, "领域名称不能为空");
        }
        return R.ok(service.create(entity));
    }

    @RequirePermission("masterdata:update")
    @PutMapping("/{id}")
    public R<TerminologyDomainEntity> update(@PathVariable Long id,
                                              @RequestBody TerminologyDomainEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @RequirePermission("masterdata:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}
