package com.maidc.model.controller;

import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.model.dto.EvaluationCreateDTO;
import com.maidc.model.service.EvaluationService;
import com.maidc.model.vo.EvaluationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @OperLog(module = "model", operation = "evaluate")
    @PreAuthorize("hasPermission('model:evaluate')")
    @PostMapping
    public R<EvaluationVO> createEvaluation(@RequestBody @Valid EvaluationCreateDTO dto) {
        return R.ok(evaluationService.createEvaluation(dto));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/{id}")
    public R<EvaluationVO> getEvaluation(@PathVariable Long id) {
        return R.ok(evaluationService.getEvaluation(id));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/{id}/report")
    public R<String> getEvaluationReport(@PathVariable Long id) {
        return R.ok(evaluationService.getEvaluationReportUrl(id));
    }
}
