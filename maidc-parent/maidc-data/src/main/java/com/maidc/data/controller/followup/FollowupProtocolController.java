package com.maidc.data.controller.followup;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.FollowupProtocolEntity;
import com.maidc.data.service.followup.FollowupProtocolService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 随访方案 API（/api/v1/cdr/disease-cohorts/{id}/protocols）
 * manage: 方案定义/发布新版本
 */
@RestController
@RequestMapping("/api/v1/cdr/disease-cohorts/{cohortId}/protocols")
@RequiredArgsConstructor
public class FollowupProtocolController {

    private final FollowupProtocolService service;

    @PreAuthorize("hasPermission('disease:followup:manage')")
    @GetMapping
    public R<List<FollowupProtocolEntity>> list(@PathVariable Long cohortId) {
        return R.ok(service.list(cohortId));
    }

    @PreAuthorize("hasPermission('disease:followup:manage')")
    @GetMapping("/latest")
    public R<FollowupProtocolEntity> latest(@PathVariable Long cohortId) {
        return R.ok(service.latestPublished(cohortId));
    }

    @PreAuthorize("hasPermission('disease:followup:manage')")
    @PostMapping
    public R<FollowupProtocolEntity> create(@PathVariable Long cohortId, @RequestBody Map<String, Object> req) {
        return R.ok(service.create(cohortId, req, false));
    }

    /** PUT = 发布新版本（存量档案快照不受影响） */
    @PreAuthorize("hasPermission('disease:followup:manage')")
    @PutMapping
    public R<FollowupProtocolEntity> publishNewVersion(@PathVariable Long cohortId, @RequestBody Map<String, Object> req) {
        return R.ok(service.publishNewVersion(cohortId, req));
    }
}
