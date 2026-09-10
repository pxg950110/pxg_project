package com.maidc.data.controller.followup;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.PatientFollowupEntity;
import com.maidc.data.service.followup.FollowupTaskService;
import com.maidc.data.service.followup.PatientFollowupService;
import com.maidc.data.service.followup.TreatmentRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 随访档案聚合详情：档案头（患者/医护名）+ 任务时间轴 + 评估记录 + 治疗记录，一次拉全。
 */
@RestController
@RequestMapping("/api/v1/cdr/followups/{id}/detail")
@RequiredArgsConstructor
public class FollowupDetailController {

    private final PatientFollowupService followupService;
    private final FollowupTaskService taskService;
    private final TreatmentRecordService treatmentService;

    @PreAuthorize("hasPermission('disease:followup:work')")
    @GetMapping
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        PatientFollowupEntity f = followupService.get(id); // 含 DEPT fail-closed 校验 + 展示名填充
        Map<String, Object> result = new HashMap<>();
        result.put("followup", f);
        result.put("tasks", taskService.timeline(id));
        result.put("assessments", taskService.assessments(id));
        result.put("treatments", treatmentService.list(id));
        return R.ok(result);
    }
}
