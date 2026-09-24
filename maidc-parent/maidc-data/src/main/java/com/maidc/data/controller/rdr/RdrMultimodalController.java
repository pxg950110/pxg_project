package com.maidc.data.controller.rdr;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.rdr.RdrMultimodalLinkEntity;
import com.maidc.data.entity.rdr.RdrMultimodalLinkGroupEntity;
import com.maidc.data.entity.rdr.RdrCompletenessAssessmentEntity;
import com.maidc.data.service.rdr.RdrMultimodalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * RDR多模态数据Controller
 */
@RestController
@RequestMapping("/api/v1/rdr/multimodal")
@RequiredArgsConstructor
public class RdrMultimodalController {

    private final RdrMultimodalService service;

    // ── 多模态关联 ──

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/links")
    public R<Page<RdrMultimodalLinkEntity>> listLinks(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long encounterId,
            @RequestParam(required = false) String modalityType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.listLinks(patientId, encounterId, modalityType, status, page, pageSize));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/links/{id}")
    public R<RdrMultimodalLinkEntity> getLink(@PathVariable Long id) {
        return R.ok(service.getLinkById(id));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/patients/{patientId}/links")
    public R<List<RdrMultimodalLinkEntity>> getLinksByPatient(@PathVariable Long patientId) {
        return R.ok(service.getLinksByPatient(patientId));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/patients/{patientId}/links/time-range")
    public R<List<RdrMultimodalLinkEntity>> getLinksByTimeRange(
            @PathVariable Long patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return R.ok(service.getLinksByTimeRange(patientId, start, end));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/patients/{patientId}/modality-count")
    public R<List<Object[]>> getModalityCountForPatient(@PathVariable Long patientId) {
        return R.ok(service.getModalityCountForPatient(patientId));
    }

    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/links")
    public R<RdrMultimodalLinkEntity> createLink(@RequestBody RdrMultimodalLinkEntity entity) {
        return R.ok(service.createLink(entity));
    }

    @PreAuthorize("hasPermission('rdr:update')")
    @PostMapping("/links/{id}/validate")
    public R<RdrMultimodalLinkEntity> validateLink(@PathVariable Long id, @RequestParam String validatedBy) {
        return R.ok(service.validateLink(id, validatedBy));
    }

    @PreAuthorize("hasPermission('rdr:update')")
    @PostMapping("/links/{id}/deactivate")
    public R<Void> deactivateLink(@PathVariable Long id) {
        service.deactivateLink(id);
        return R.ok();
    }

    // ── 关联组 ──

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/groups/{id}")
    public R<RdrMultimodalLinkGroupEntity> getGroup(@PathVariable Long id) {
        return R.ok(service.getGroupById(id));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/patients/{patientId}/groups")
    public R<List<RdrMultimodalLinkGroupEntity>> getGroupsByPatient(@PathVariable Long patientId) {
        return R.ok(service.getGroupsByPatient(patientId));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/patients/{patientId}/groups/active")
    public R<List<RdrMultimodalLinkGroupEntity>> getActiveGroupsForPatient(@PathVariable Long patientId) {
        return R.ok(service.getActiveGroupsForPatient(patientId));
    }

    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/groups")
    public R<RdrMultimodalLinkGroupEntity> createGroup(@RequestBody RdrMultimodalLinkGroupEntity entity) {
        return R.ok(service.createGroup(entity));
    }

    // ── 完整性评估 ──

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/assessments/{id}")
    public R<RdrCompletenessAssessmentEntity> getAssessment(@PathVariable Long id) {
        return R.ok(service.getAssessmentById(id));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/patients/{patientId}/assessments")
    public R<List<RdrCompletenessAssessmentEntity>> getAssessmentsByPatient(@PathVariable Long patientId) {
        return R.ok(service.getAssessmentsByPatient(patientId));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/patients/{patientId}/assessments/latest")
    public R<RdrCompletenessAssessmentEntity> getLatestAssessment(@PathVariable Long patientId) {
        RdrCompletenessAssessmentEntity assessment = service.getLatestAssessment(patientId);
        return assessment != null ? R.ok(assessment) : R.ok(null);
    }

    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/assessments")
    public R<RdrCompletenessAssessmentEntity> createAssessment(@RequestBody RdrCompletenessAssessmentEntity entity) {
        return R.ok(service.createAssessment(entity));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/patients/{patientId}/completeness-stats")
    public R<Map<String, Object>> getCompletenessStats(@PathVariable Long patientId) {
        return R.ok(service.getCompletenessStats(patientId));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/assessments/average-score")
    public R<Double> getAverageCompletenessScore() {
        return R.ok(service.getAverageCompletenessScore());
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/assessments/distribution")
    public R<List<Object[]>> getCompletenessDistribution() {
        return R.ok(service.getCompletenessDistribution());
    }
}