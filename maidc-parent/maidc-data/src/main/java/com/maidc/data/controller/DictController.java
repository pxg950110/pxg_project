package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.common.security.annotation.RequirePermission;
import com.maidc.data.entity.DictEntity;
import com.maidc.data.entity.DictTypeEntity;
import com.maidc.data.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据字典接口：字典类型（system.s_dict_type）与字典项（system.s_dict）。
 * 类型即实体（id/code/name/remark），条目按类型 ID 组织。
 */
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    // ---------- 字典类型 ----------

    @GetMapping("/dict-types")
    public R<Page<DictTypeEntity>> listTypes(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(dictService.listTypes(keyword, page, pageSize));
    }

    @GetMapping("/dict-types/{id}")
    public R<DictTypeEntity> getType(@PathVariable Long id) {
        return R.ok(dictService.getType(id));
    }

    @RequirePermission("system:dict:manage")
    @PostMapping("/dict-types")
    public R<DictTypeEntity> createType(@RequestBody DictTypeEntity entity) {
        return R.ok(dictService.createType(entity));
    }

    @RequirePermission("system:dict:manage")
    @PutMapping("/dict-types/{id}")
    public R<DictTypeEntity> updateType(@PathVariable Long id, @RequestBody DictTypeEntity updates) {
        return R.ok(dictService.updateType(id, updates));
    }

    @RequirePermission("system:dict:manage")
    @DeleteMapping("/dict-types/{id}")
    public R<Void> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return R.ok();
    }

    // ---------- 字典项 ----------

    @GetMapping("/dict-types/{id}/items")
    public R<List<DictEntity>> listItems(@PathVariable Long id,
                                         @RequestParam(required = false) String keyword) {
        return R.ok(dictService.listItems(id, keyword));
    }

    @RequirePermission("system:dict:manage")
    @PostMapping("/dict-types/{id}/items")
    public R<DictEntity> createItem(@PathVariable Long id, @RequestBody DictEntity item) {
        return R.ok(dictService.createItem(id, item));
    }

    @RequirePermission("system:dict:manage")
    @PutMapping("/dict-items/{id}")
    public R<DictEntity> updateItem(@PathVariable Long id, @RequestBody DictEntity updates) {
        return R.ok(dictService.updateItem(id, updates));
    }

    @RequirePermission("system:dict:manage")
    @DeleteMapping("/dict-items/{id}")
    public R<Void> deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id);
        return R.ok();
    }
}
