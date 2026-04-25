package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.DrugInteractionEntity;
import com.maidc.data.service.DrugInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/drug-interactions")
@RequiredArgsConstructor
public class DrugInteractionController {

    private final DrugInteractionService service;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping
    public R<List<DrugInteractionEntity>> list(
            @RequestParam(required = false) Long drug1,
            @RequestParam(required = false) Long drug2,
            @RequestParam(required = false) String severity) {
        return R.ok(service.list(drug1, drug2, severity));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/check")
    public R<List<DrugInteractionEntity>> check(
            @RequestParam Long drug1,
            @RequestParam Long drug2) {
        return R.ok(service.checkPair(drug1, drug2));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @PostMapping("/check-list")
    public R<List<DrugInteractionEntity>> checkList(@RequestBody List<Long> drugIds) {
        if (drugIds == null || drugIds.size() < 2) {
            return R.ok(List.of());
        }
        return R.ok(service.checkList(drugIds));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping
    public R<DrugInteractionEntity> create(@RequestBody DrugInteractionEntity entity) {
        if (entity.getDrugConceptId1() == null || entity.getDrugConceptId2() == null) {
            return R.fail(400, "两个药物概念ID均不能为空");
        }
        if (entity.getSeverity() == null || entity.getSeverity().isBlank()) {
            return R.fail(400, "严重程度不能为空");
        }
        return R.ok(service.create(entity));
    }
}
