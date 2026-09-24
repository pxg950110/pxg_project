package com.maidc.data.controller.cdr;

import com.maidc.common.core.result.R;
import com.maidc.data.service.cdr.Patient360Service;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * CDR患者360视图Controller
 */
@RestController
@RequestMapping("/api/v1/cdr/patient-360")
@RequiredArgsConstructor
public class Patient360Controller {

    private final Patient360Service service;

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{patientId}")
    public R<Map<String, Object>> getPatient360(@PathVariable Long patientId) {
        return R.ok(service.getPatient360(patientId));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{patientId}/basic-info")
    public R<Map<String, Object>> getBasicInfo(@PathVariable Long patientId) {
        return R.ok(service.getPatientBasicInfo(patientId));
    }

    /** 诊断地图：诊断按 ICD 章节聚合到身体系统（附热区坐标） */
    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{patientId}/diagnosis-map")
    public R<List<Map<String, Object>>> getDiagnosisMap(@PathVariable Long patientId) {
        return R.ok(service.getDiagnosisMap(patientId));
    }

    /** 最近异常体检指标（direction=偏高/偏低/异常，由结果值对比参考范围推导） */
    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{patientId}/abnormal-indicators")
    public R<Map<String, Object>> getAbnormalIndicators(@PathVariable Long patientId,
                                                        @RequestParam(defaultValue = "8") int limit) {
        return R.ok(service.getRecentAbnormalIndicators(patientId, limit));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{patientId}/encounter-stats")
    public R<Map<String, Object>> getEncounterStats(@PathVariable Long patientId) {
        return R.ok(service.getEncounterStats(patientId));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{patientId}/diagnosis-stats")
    public R<Map<String, Object>> getDiagnosisStats(@PathVariable Long patientId) {
        return R.ok(service.getDiagnosisStats(patientId));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{patientId}/lab-stats")
    public R<Map<String, Object>> getLabStats(@PathVariable Long patientId) {
        return R.ok(service.getLabStats(patientId));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{patientId}/imaging-stats")
    public R<Map<String, Object>> getImagingStats(@PathVariable Long patientId) {
        return R.ok(service.getImagingStats(patientId));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{patientId}/allergies")
    public R<List<Map<String, Object>>> getAllergies(@PathVariable Long patientId) {
        return R.ok(service.getAllergies(patientId));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{patientId}/family-history")
    public R<List<Map<String, Object>>> getFamilyHistory(@PathVariable Long patientId) {
        return R.ok(service.getFamilyHistory(patientId));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{patientId}/timeline")
    public R<List<Map<String, Object>>> getTimeline(
            @PathVariable Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusYears(5);
        if (endDate == null) endDate = LocalDate.now();
        return R.ok(service.getTimeline(patientId, startDate, endDate));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{patientId}/completeness-score")
    public R<BigDecimal> getCompletenessScore(@PathVariable Long patientId) {
        return R.ok(service.calculateCompletenessScore(patientId));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/search")
    public R<List<Map<String, Object>>> searchPatients(
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String diagCodes,
            @RequestParam(required = false) String encounterType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.searchPatients(gender, minAge, maxAge, diagCodes, encounterType, startTime, endTime, page, pageSize));
    }
}