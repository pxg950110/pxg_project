package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.dto.etl.EtlStepCreateDTO;
import com.maidc.data.dto.etl.EtlStepUpdateDTO;
import com.maidc.data.service.etl.EtlStepService;
import com.maidc.data.vo.EtlStepVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cdr/etl/pipelines/{pipelineId}/steps")
@RequiredArgsConstructor
public class EtlStepController {

    private final EtlStepService stepService;

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping
    public R<List<EtlStepVO>> listSteps(@PathVariable Long pipelineId) {
        return R.ok(stepService.listSteps(pipelineId));
    }

    @PreAuthorize("hasPermission('data:create')")
    @PostMapping
    public R<EtlStepVO> createStep(@PathVariable Long pipelineId,
                                   @RequestBody @Valid EtlStepCreateDTO dto) {
        return R.ok(stepService.createStep(pipelineId, dto));
    }

    @PreAuthorize("hasPermission('data:update')")
    @PutMapping("/{stepId}")
    public R<EtlStepVO> updateStep(@PathVariable Long stepId,
                                   @RequestBody EtlStepUpdateDTO dto) {
        return R.ok(stepService.updateStep(stepId, dto));
    }

    @PreAuthorize("hasPermission('data:delete')")
    @DeleteMapping("/{stepId}")
    public R<Void> deleteStep(@PathVariable Long stepId) {
        stepService.deleteStep(stepId);
        return R.ok();
    }

    @SuppressWarnings("unchecked")
    @PreAuthorize("hasPermission('data:update')")
    @PutMapping("/reorder")
    public R<Void> reorderSteps(@PathVariable Long pipelineId,
                                @RequestBody Map<String, List<Long>> body) {
        stepService.reorderSteps(pipelineId, body.get("stepIds"));
        return R.ok();
    }

    @PreAuthorize("hasPermission('data:read')")
    @PostMapping("/{stepId}/preview")
    public R<List<Map<String, Object>>> previewData(@PathVariable Long stepId) {
        return R.ok(stepService.previewData(stepId));
    }
}
