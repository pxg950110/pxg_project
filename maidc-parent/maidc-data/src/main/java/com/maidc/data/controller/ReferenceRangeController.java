package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.ReferenceRangeEntity;
import com.maidc.data.service.ReferenceRangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/reference-ranges")
@RequiredArgsConstructor
public class ReferenceRangeController {

    private final ReferenceRangeService service;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping
    public R<List<ReferenceRangeEntity>> list(
            @RequestParam(required = false) Long conceptId,
            @RequestParam(required = false) String gender) {
        return R.ok(service.list(conceptId, gender));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}")
    public R<ReferenceRangeEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/evaluate")
    public R<ReferenceRangeEntity> evaluate(
            @RequestParam Long conceptId,
            @RequestParam String gender,
            @RequestParam BigDecimal age) {
        ReferenceRangeEntity result = service.evaluate(conceptId, gender, age);
        if (result == null) {
            return R.fail(404, "未找到匹配的参考范围");
        }
        return R.ok(result);
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping
    public R<ReferenceRangeEntity> create(@RequestBody ReferenceRangeEntity entity) {
        if (entity.getConceptId() == null) {
            return R.fail(400, "概念ID不能为空");
        }
        return R.ok(service.create(entity));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PutMapping("/{id}")
    public R<ReferenceRangeEntity> update(@PathVariable Long id, @RequestBody ReferenceRangeEntity entity) {
        return R.ok(service.update(id, entity));
    }
}
