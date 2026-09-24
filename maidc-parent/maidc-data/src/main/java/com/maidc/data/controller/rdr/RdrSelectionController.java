package com.maidc.data.controller.rdr;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.rdr.RdrSelectionConfigEntity;
import com.maidc.data.entity.rdr.RdrSelectionResultEntity;
import com.maidc.data.entity.rdr.RdrCohortMemberSnapshotEntity;
import com.maidc.data.service.rdr.RdrSelectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RDR队列筛选Controller
 */
@RestController
@RequestMapping("/api/v1/rdr/selections")
@RequiredArgsConstructor
public class RdrSelectionController {

    private final RdrSelectionService service;

    // ── 筛选配置 ──

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/configs")
    public R<Page<RdrSelectionConfigEntity>> listConfigs(
            @RequestParam(required = false) Long cohortId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.listConfigs(cohortId, status, page, pageSize));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/configs/{id}")
    public R<RdrSelectionConfigEntity> getConfig(@PathVariable Long id) {
        return R.ok(service.getConfigById(id));
    }

    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/configs")
    public R<RdrSelectionConfigEntity> createConfig(@RequestBody RdrSelectionConfigEntity entity) {
        return R.ok(service.createConfig(entity));
    }

    @PreAuthorize("hasPermission('rdr:update')")
    @PutMapping("/configs/{id}")
    public R<RdrSelectionConfigEntity> updateConfig(@PathVariable Long id, @RequestBody RdrSelectionConfigEntity entity) {
        return R.ok(service.updateConfig(id, entity));
    }

    @PreAuthorize("hasPermission('rdr:update')")
    @PostMapping("/configs/{id}/activate")
    public R<RdrSelectionConfigEntity> activateConfig(@PathVariable Long id) {
        return R.ok(service.activateConfig(id));
    }

    // ── 筛选执行 ──

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/results")
    public R<Page<RdrSelectionResultEntity>> listResults(
            @RequestParam(required = false) Long selectionId,
            @RequestParam(required = false) Long cohortId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.listResults(selectionId, cohortId, status, page, pageSize));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/results/{id}")
    public R<RdrSelectionResultEntity> getResult(@PathVariable Long id) {
        return R.ok(service.getResultById(id));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/cohorts/{cohortId}/latest-result")
    public R<RdrSelectionResultEntity> getLatestCompletedResult(@PathVariable Long cohortId) {
        RdrSelectionResultEntity result = service.getLatestCompletedResult(cohortId);
        return result != null ? R.ok(result) : R.ok(null);
    }

    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/start")
    public R<RdrSelectionResultEntity> startSelection(
            @RequestParam Long selectionId,
            @RequestParam(required = false) String triggeredBy) {
        return R.ok(service.startSelection(selectionId, triggeredBy));
    }

    // ── 成员快照 ──

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/results/{resultId}/members")
    public R<List<RdrCohortMemberSnapshotEntity>> getMembersByResult(@PathVariable Long resultId) {
        return R.ok(service.getMembersByResult(resultId));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/cohorts/{cohortId}/members")
    public R<List<RdrCohortMemberSnapshotEntity>> getMembersByCohort(@PathVariable Long cohortId) {
        return R.ok(service.getMembersByCohort(cohortId));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/results/{resultId}/member-count")
    public R<Long> countActiveMembers(@PathVariable Long resultId) {
        return R.ok(service.countActiveMembers(resultId));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/results/{resultId}/stats")
    public R<Map<String, Object>> getMemberStats(@PathVariable Long resultId) {
        return R.ok(service.getStats(resultId));
    }
}