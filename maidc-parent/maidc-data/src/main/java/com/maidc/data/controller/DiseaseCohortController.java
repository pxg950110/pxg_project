package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.DiseaseCohortEntity;
import com.maidc.data.service.DiseaseAiService;
import com.maidc.data.service.DiseaseCohortService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cdr/disease-cohorts")
@RequiredArgsConstructor
public class DiseaseCohortController {

    private final DiseaseCohortService service;
    private final DiseaseAiService diseaseAiService;

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping
    public R<Page<DiseaseCohortEntity>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        return R.ok(service.listCohorts(keyword, status, page, page_size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}")
    public R<DiseaseCohortEntity> get(@PathVariable Long id) {
        return R.ok(service.getCohort(id));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping
    public R<DiseaseCohortEntity> create(@RequestBody DiseaseCohortEntity entity) {
        if (entity.getName() == null || entity.getName().isBlank()) {
            return R.fail(400, "专病库名称不能为空");
        }
        if (entity.getInclusionRules() == null || entity.getInclusionRules().isBlank()) {
            return R.fail(400, "纳入规则不能为空");
        }
        return R.ok(service.createCohort(entity));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PutMapping("/{id}")
    public R<DiseaseCohortEntity> update(@PathVariable Long id, @RequestBody DiseaseCohortEntity entity) {
        return R.ok(service.updateCohort(id, entity));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.deleteCohort(id);
        return R.ok();
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/{id}/sync")
    public R<Void> sync(@PathVariable Long id) {
        service.matchPatients(id);
        return R.ok();
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}/match-preview")
    public R<Map<String, Object>> matchPreview(@PathVariable Long id) {
        int count = service.matchPreview(id);
        return R.ok(Map.of("patientCount", count));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}/patients")
    public R<Map<String, Object>> patients(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        return R.ok(service.getPatients(id, page, page_size));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/{id}/patients/{patientId}")
    public R<Void> addPatient(@PathVariable Long id, @PathVariable Long patientId) {
        service.addPatient(id, patientId);
        return R.ok();
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/{id}/patients/{patientId}")
    public R<Void> removePatient(@PathVariable Long id, @PathVariable Long patientId) {
        service.removePatient(id, patientId);
        return R.ok();
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}/statistics")
    public R<Map<String, Object>> statistics(@PathVariable Long id) {
        return R.ok(service.getStatistics(id));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}/export")
    public void export(@PathVariable Long id, HttpServletResponse response) throws IOException {
        service.exportCsv(id, response);
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @PostMapping("/ai-suggest")
    public R<Map<String, Object>> aiSuggest(@RequestBody Map<String, String> req) {
        return R.ok(diseaseAiService.suggestRules(req.get("disease_name")));
    }
}
