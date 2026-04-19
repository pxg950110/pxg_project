package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.DataSourceTypeEntity;
import com.maidc.data.service.DataSourceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cdr/datasource-types")
@RequiredArgsConstructor
public class DataSourceTypeController {

    private final DataSourceTypeService dataSourceTypeService;

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping
    public R<List<DataSourceTypeEntity>> listTypes() {
        return R.ok(dataSourceTypeService.listAll());
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{code}")
    public R<DataSourceTypeEntity> getType(@PathVariable String code) {
        DataSourceTypeEntity entity = dataSourceTypeService.getByTypeCode(code);
        if (entity == null) return R.fail(404, "类型不存在");
        return R.ok(entity);
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping
    public R<DataSourceTypeEntity> createType(@RequestBody DataSourceTypeEntity entity) {
        return R.ok(dataSourceTypeService.create(entity));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PutMapping("/{code}")
    public R<DataSourceTypeEntity> updateType(@PathVariable String code,
                                               @RequestBody DataSourceTypeEntity entity) {
        DataSourceTypeEntity result = dataSourceTypeService.update(code, entity);
        if (result == null) return R.fail(404, "类型不存在");
        return R.ok(result);
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/{code}")
    public R<Void> deleteType(@PathVariable String code) {
        try {
            dataSourceTypeService.delete(code);
            return R.ok();
        } catch (IllegalStateException e) {
            return R.fail(400, e.getMessage());
        }
    }
}
