package com.maidc.model.controller;

import com.maidc.common.core.result.R;
import com.maidc.model.dto.InferenceRequestDTO;
import com.maidc.model.service.InferenceService;
import com.maidc.model.vo.InferenceResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inference")
@RequiredArgsConstructor
public class InferenceController {

    private final InferenceService inferenceService;

    @PreAuthorize("hasPermission('model:deploy')")
    @PostMapping("/{deploymentId}")
    public R<InferenceResultVO> inference(@PathVariable Long deploymentId,
                                           @RequestBody @Valid InferenceRequestDTO dto) {
        return R.ok(inferenceService.inference(deploymentId, dto));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/models")
    public R<List<Map<String, Object>>> listModels() {
        return R.ok(Collections.emptyList());
    }
}
