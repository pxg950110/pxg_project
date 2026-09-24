package com.maidc.data.controller.followup;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.PatientFollowupEntity;
import com.maidc.data.service.followup.PatientFollowupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 患者随访档案 API：建档 / 列表 / 状态机 / 升级方案 / 结局统计
 * manage: 建档/分配/结案/升级；结局看板 manage
 */
@RestController
@RequiredArgsConstructor
public class PatientFollowupController {

    private final PatientFollowupService followupService;
    private final com.maidc.data.service.followup.OutcomeStatsService statsService;

    @PreAuthorize("hasPermission('disease:followup:manage')")
    @PostMapping("/api/v1/cdr/disease-cohorts/{cohortId}/followups")
    public R<Map<String, Object>> enroll(@PathVariable Long cohortId, @RequestBody Map<String, Object> req) {
        return R.ok(followupService.enroll(cohortId, req));
    }

    @PreAuthorize("hasPermission('disease:followup:manage')")
    @GetMapping("/api/v1/cdr/disease-cohorts/{cohortId}/followups")
    public R<Page<PatientFollowupEntity>> list(@PathVariable Long cohortId,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int page_size) {
        return R.ok(followupService.list(cohortId, status, page, page_size));
    }

    @PreAuthorize("hasPermission('disease:followup:manage')")
    @PostMapping("/api/v1/cdr/followups/{id}/close")
    public R<PatientFollowupEntity> close(@PathVariable Long id, @RequestBody Map<String, String> req) {
        return R.ok(followupService.close(id, req.get("reason")));
    }

    @PreAuthorize("hasPermission('disease:followup:manage')")
    @PostMapping("/api/v1/cdr/followups/{id}/suspend")
    public R<PatientFollowupEntity> suspend(@PathVariable Long id) {
        return R.ok(followupService.suspend(id));
    }

    @PreAuthorize("hasPermission('disease:followup:manage')")
    @PostMapping("/api/v1/cdr/followups/{id}/resume")
    public R<PatientFollowupEntity> resume(@PathVariable Long id) {
        return R.ok(followupService.resume(id));
    }

    /** 升级方案：只重建未来未完成任务 */
    @PreAuthorize("hasPermission('disease:followup:manage')")
    @PostMapping("/api/v1/cdr/followups/{id}/upgrade-protocol")
    public R<Map<String, Object>> upgradeProtocol(@PathVariable Long id) {
        return R.ok(followupService.upgradeProtocol(id));
    }

    @PreAuthorize("hasPermission('disease:followup:manage')")
    @GetMapping("/api/v1/cdr/disease-cohorts/{cohortId}/outcome-stats")
    public R<Map<String, Object>> outcomeStats(@PathVariable Long cohortId) {
        return R.ok(statsService.stats(cohortId));
    }
}
