package com.maidc.data.controller.dictionary;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.dictionary.DrugCategoryEntity;
import com.maidc.data.entity.dictionary.DrugEntity;
import com.maidc.data.service.dictionary.DrugService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import com.maidc.common.security.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/drugs")
@RequiredArgsConstructor
public class DrugController {

    private final DrugService service;

    @GetMapping
    public R<Page<DrugEntity>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String insuranceType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(categoryId, status, insuranceType, keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public R<DrugEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @GetMapping("/code/{code}")
    public R<DrugEntity> getByCode(@PathVariable String code) {
        return R.ok(service.getByCode(code));
    }

    @GetMapping("/search")
    public R<Page<DrugEntity>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.search(keyword, page, pageSize));
    }

    @GetMapping("/by-atc/{atcCode}")
    public R<List<DrugEntity>> getByAtcCode(@PathVariable String atcCode) {
        return R.ok(service.getByAtcCode(atcCode));
    }

    @RequirePermission("masterdata:create")
    @PostMapping
    public R<DrugEntity> create(@RequestBody DrugEntity entity) {
        return R.ok(service.create(entity));
    }

    @RequirePermission("masterdata:update")
    @PutMapping("/{id}")
    public R<DrugEntity> update(@PathVariable Long id, @RequestBody DrugEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @RequirePermission("masterdata:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    // ==================== 分类管理 ====================

    @GetMapping("/categories")
    public R<List<DrugCategoryEntity>> getCategoryTree() {
        return R.ok(service.getCategoryTree());
    }

    @GetMapping("/categories/root")
    public R<List<DrugCategoryEntity>> getRootCategories() {
        return R.ok(service.getRootCategories());
    }

    @GetMapping("/categories/{parentId}/children")
    public R<List<DrugCategoryEntity>> getChildCategories(@PathVariable Long parentId) {
        return R.ok(service.getChildCategories(parentId));
    }

    @RequirePermission("masterdata:create")
    @PostMapping("/categories")
    public R<DrugCategoryEntity> createCategory(@RequestBody DrugCategoryEntity entity) {
        return R.ok(service.createCategory(entity));
    }

    @RequirePermission("masterdata:delete")
    @DeleteMapping("/categories/{id}")
    public R<Void> deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
        return R.ok();
    }
}