package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.common.security.annotation.RequirePermission;
import com.maidc.data.entity.ConceptDomainEntity;
import com.maidc.data.entity.ValueMeaningEntity;
import com.maidc.data.service.ConceptDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 概念域（WS/T 303-2023 CD）接口：CRUD + 值含义子资源。
 * 供门户「数据标准体系」概念域页面使用。
 */
@RestController
@RequestMapping("/api/v1/masterdata/concept-domains")
@RequiredArgsConstructor
public class ConceptDomainController {

    private final ConceptDomainService service;

    @RequirePermission("masterdata:read")
    @GetMapping
    public R<Page<ConceptDomainEntity>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String domainType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(required = false) Integer size) {
        int effectiveSize = pageSize != null ? pageSize : (size != null ? size : 20);
        return R.ok(service.list(keyword, domainType, page, effectiveSize));
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/all")
    public R<List<ConceptDomainEntity>> listAll() {
        return R.ok(service.listAll());
    }

    @RequirePermission("masterdata:read")
    @GetMapping("/{id}")
    public R<ConceptDomainEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @RequirePermission("masterdata:create")
    @PostMapping
    public R<ConceptDomainEntity> create(@RequestBody ConceptDomainEntity entity) {
        return R.ok(service.create(entity));
    }

    @RequirePermission("masterdata:update")
    @PutMapping("/{id}")
    public R<ConceptDomainEntity> update(@PathVariable Long id, @RequestBody ConceptDomainEntity updates) {
        return R.ok(service.update(id, updates));
    }

    @RequirePermission("masterdata:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    // ---------- 值含义子资源 ----------

    @RequirePermission("masterdata:read")
    @GetMapping("/{conceptDomainId}/value-meanings")
    public R<List<ValueMeaningEntity>> listValueMeanings(
            @PathVariable Long conceptDomainId,
            @RequestParam(required = false) String keyword) {
        return R.ok(service.listValueMeanings(conceptDomainId, keyword));
    }

    @RequirePermission("masterdata:create")
    @PostMapping("/{conceptDomainId}/value-meanings")
    public R<ValueMeaningEntity> addValueMeaning(@PathVariable Long conceptDomainId,
                                                 @RequestBody ValueMeaningEntity vm) {
        return R.ok(service.addValueMeaning(conceptDomainId, vm));
    }

    @RequirePermission("masterdata:update")
    @PutMapping("/value-meanings/{id}")
    public R<ValueMeaningEntity> updateValueMeaning(@PathVariable Long id, @RequestBody ValueMeaningEntity updates) {
        return R.ok(service.updateValueMeaning(id, updates));
    }

    @RequirePermission("masterdata:delete")
    @DeleteMapping("/value-meanings/{id}")
    public R<Void> deleteValueMeaning(@PathVariable Long id) {
        service.deleteValueMeaning(id);
        return R.ok();
    }
}
