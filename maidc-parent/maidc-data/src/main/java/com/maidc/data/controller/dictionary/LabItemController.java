package com.maidc.data.controller.dictionary;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.dictionary.LabItemEntity;
import com.maidc.data.service.dictionary.LabItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import com.maidc.common.security.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/lab-items")
@RequiredArgsConstructor
public class LabItemController {

    private final LabItemService service;

    @GetMapping
    public R<Page<LabItemEntity>> list(
            @RequestParam(required = false) String labCategory,
            @RequestParam(required = false) String specimenType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(labCategory, specimenType, keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public R<LabItemEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @GetMapping("/by-loinc/{loincCode}")
    public R<LabItemEntity> getByLoincCode(@PathVariable String loincCode) {
        return R.ok(service.getByLoincCode(loincCode));
    }

    @GetMapping("/search")
    public R<Page<LabItemEntity>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.search(keyword, page, pageSize));
    }

    @GetMapping("/categories")
    public R<List<String>> getAllCategories() {
        return R.ok(service.getAllCategories());
    }

    @GetMapping("/{parentId}/children")
    public R<List<LabItemEntity>> getChildren(@PathVariable Long parentId) {
        return R.ok(service.getChildren(parentId));
    }

    @RequirePermission("masterdata:create")
    @PostMapping
    public R<LabItemEntity> create(@RequestBody LabItemEntity entity) {
        return R.ok(service.create(entity));
    }

    @RequirePermission("masterdata:update")
    @PutMapping("/{id}")
    public R<LabItemEntity> update(@PathVariable Long id, @RequestBody LabItemEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @RequirePermission("masterdata:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}