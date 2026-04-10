package com.maidc.model.controller;

import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.model.dto.ModelCreateDTO;
import com.maidc.model.dto.ModelUpdateDTO;
import com.maidc.model.service.ModelService;
import com.maidc.model.vo.ModelDetailVO;
import com.maidc.model.vo.ModelVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    @OperLog(module = "model", operation = "create")
    @PreAuthorize("hasPermission('model:create')")
    @PostMapping
    public R<ModelVO> createModel(@RequestBody @Valid ModelCreateDTO dto) {
        return R.ok(modelService.createModel(dto));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping
    public R<PageResult<ModelVO>> listModels(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "0") Long orgId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String model_type,
            @RequestParam(required = false) String status) {
        return R.ok(modelService.listModels(page, pageSize, orgId, keyword, model_type, status));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/{id}")
    public R<ModelDetailVO> getModel(@PathVariable Long id) {
        return R.ok(modelService.getModelDetail(id));
    }

    @OperLog(module = "model", operation = "update")
    @PreAuthorize("hasPermission('model:create')")
    @PutMapping("/{id}")
    public R<ModelVO> updateModel(@PathVariable Long id, @RequestBody ModelUpdateDTO dto) {
        return R.ok(modelService.updateModel(id, dto));
    }

    @OperLog(module = "model", operation = "delete")
    @PreAuthorize("hasPermission('model:create')")
    @DeleteMapping("/{id}")
    public R<Void> deleteModel(@PathVariable Long id) {
        modelService.deleteModel(id);
        return R.ok();
    }
}
