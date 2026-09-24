package com.maidc.data.controller.followup;

import com.maidc.common.core.result.R;
import com.maidc.data.service.followup.FollowupTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 随访任务 API：工作台 / 时间轴 / 执行（complete=work, skip=manage）/ 计划外评估（manage）
 */
@RestController
@RequestMapping("/api/v1/cdr")
@RequiredArgsConstructor
public class FollowupTaskController {

    private final FollowupTaskService service;
    private final com.maidc.data.service.followup.FollowupReminderScheduler reminderScheduler;

    /** 工作台：今日到期/已超期/未来7天（OVERDUE 派生） */
    @PreAuthorize("hasPermission('disease:followup:work')")
    @GetMapping("/followup-tasks/my")
    public R<Map<String, Object>> my(@RequestParam(required = false) Long userId,
                                     @RequestParam(defaultValue = "false") boolean all) {
        return R.ok(service.myWorkbench(userId, all, LocalDate.now()));
    }

    /** 档案任务时间轴 */
    @PreAuthorize("hasPermission('disease:followup:work')")
    @GetMapping("/followups/{followupId}/tasks")
    public R<List<Map<String, Object>>> timeline(@PathVariable Long followupId) {
        return R.ok(service.timeline(followupId));
    }

    /** 手动触发随访提醒扫描（管理/联调用，与每日 8 点定时同一逻辑） */
    @PreAuthorize("hasPermission('disease:followup:manage')")
    @PostMapping("/followup-tasks/reminders/run")
    public R<Integer> runReminders(@RequestParam(required = false) String date) {
        java.time.LocalDate d = (date == null || date.isBlank()) ? java.time.LocalDate.now() : java.time.LocalDate.parse(date);
        return R.ok(reminderScheduler.runReminders(d));
    }

    /** 执行：assessments 批量 + 可选 treatments + task DONE 同一事务 */
    @PreAuthorize("hasPermission('disease:followup:work')")
    @PostMapping("/followup-tasks/{taskId}/complete")
    public R<Map<String, Object>> complete(@PathVariable Long taskId,
                                           @RequestParam(required = false) Long userId,
                                           @RequestBody Map<String, Object> req) {
        return R.ok(service.complete(taskId, req, userId));
    }

    /** 跳过（医生，原因必填） */
    @PreAuthorize("hasPermission('disease:followup:manage')")
    @PostMapping("/followup-tasks/{taskId}/skip")
    public R<Void> skip(@PathVariable Long taskId,
                        @RequestParam(required = false) Long userId,
                        @RequestBody Map<String, String> req) {
        service.skip(taskId, req.get("reason"), userId);
        return R.ok();
    }

    @PreAuthorize("hasPermission('disease:followup:work')")
    @GetMapping("/followups/{followupId}/assessments")
    public R<?> assessments(@PathVariable Long followupId) {
        return R.ok(service.assessments(followupId));
    }

    /** 计划外评估（task_id=NULL，医生随时发起） */
    @PreAuthorize("hasPermission('disease:followup:manage')")
    @PostMapping("/followups/{followupId}/assessments")
    public R<?> planFreeAssessment(@PathVariable Long followupId,
                                   @RequestParam(required = false) Long userId,
                                   @RequestBody Map<String, Object> req) {
        return R.ok(service.planFreeAssessment(followupId, req, userId));
    }
}
