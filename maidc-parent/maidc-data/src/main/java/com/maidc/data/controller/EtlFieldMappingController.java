package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.dto.etl.EtlFieldMappingDTO;
import com.maidc.data.service.etl.EtlFieldMappingService;
import com.maidc.data.vo.EtlFieldMappingVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cdr/etl/steps/{stepId}/field-mappings")
@RequiredArgsConstructor
public class EtlFieldMappingController {

    private final EtlFieldMappingService fieldMappingService;

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping
    public R<List<EtlFieldMappingVO>> listMappings(@PathVariable Long stepId) {
        return R.ok(fieldMappingService.listMappings(stepId));
    }

    @PreAuthorize("hasPermission('data:update')")
    @PutMapping
    public R<List<EtlFieldMappingVO>> batchUpdateMappings(
            @PathVariable Long stepId,
            @RequestBody @Valid List<EtlFieldMappingDTO> dtos) {
        return R.ok(fieldMappingService.batchUpdateMappings(stepId, dtos));
    }

    @PreAuthorize("hasPermission('data:update')")
    @PostMapping("/auto-map")
    public R<List<EtlFieldMappingVO>> autoMap(@PathVariable Long stepId) {
        return R.ok(fieldMappingService.autoMap(stepId));
    }
}
