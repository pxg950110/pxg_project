package com.maidc.data.controller.dictionary;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.dictionary.FeeItemEntity;
import com.maidc.data.service.dictionary.FeeItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import com.maidc.common.security.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/fee-items")
@RequiredArgsConstructor
public class FeeItemController {

    private final FeeItemService service;

    @GetMapping
    public R<Page<FeeItemEntity>> list(
            @RequestParam(required = false) String feeCategory,
            @RequestParam(required = false) String feeType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(feeCategory, feeType, keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public R<FeeItemEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @GetMapping("/search")
    public R<Page<FeeItemEntity>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.search(keyword, page, pageSize));
    }

    @GetMapping("/categories")
    public R<List<String>> getAllCategories() {
        return R.ok(service.getAllCategories());
    }

    @RequirePermission("masterdata:create")
    @PostMapping
    public R<FeeItemEntity> create(@RequestBody FeeItemEntity entity) {
        return R.ok(service.create(entity));
    }

    @RequirePermission("masterdata:update")
    @PutMapping("/{id}")
    public R<FeeItemEntity> update(@PathVariable Long id, @RequestBody FeeItemEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @RequirePermission("masterdata:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}