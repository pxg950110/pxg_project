package com.maidc.data.controller.followup;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.TreatmentRecordEntity;
import com.maidc.data.service.followup.TreatmentRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 治疗记录 API：work 权限（执行人记治疗 + CDR 带入）
 */
@RestController
@RequestMapping("/api/v1/cdr/followups/{followupId}/treatments")
@RequiredArgsConstructor
public class TreatmentRecordController {

    private final TreatmentRecordService service;

    @PreAuthorize("hasPermission('disease:followup:work')")
    @GetMapping
    public R<List<TreatmentRecordEntity>> list(@PathVariable Long followupId) {
        return R.ok(service.list(followupId));
    }

    @PreAuthorize("hasPermission('disease:followup:work')")
    @PostMapping
    public R<TreatmentRecordEntity> create(@PathVariable Long followupId, @RequestBody Map<String, Object> req) {
        return R.ok(service.create(followupId, req));
    }

    /** CDR 候选：该患者用药记录（近 180 天），标注已存在重复项 */
    @PreAuthorize("hasPermission('disease:followup:work')")
    @GetMapping("/cdr-candidates")
    public R<List<Map<String, Object>>> cdrCandidates(@PathVariable Long followupId) {
        return R.ok(service.cdrCandidates(followupId));
    }

    /** 勾选带入：source=CDR 只读快照，重复自动跳过并列出 */
    @PreAuthorize("hasPermission('disease:followup:work')")
    @PostMapping("/import-cdr")
    public R<Map<String, Object>> importCdr(@PathVariable Long followupId,
                                            @RequestBody Map<String, Object> req) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) req.getOrDefault("items", List.of());
        return R.ok(service.importCdr(followupId, items));
    }
}
