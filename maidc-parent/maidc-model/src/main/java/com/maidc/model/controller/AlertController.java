package com.maidc.model.controller;

import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.model.dto.AlertRuleCreateDTO;
import com.maidc.model.service.AlertService;
import com.maidc.model.vo.AlertRecordVO;
import com.maidc.model.vo.AlertRuleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @OperLog(module = "alert", operation = "createRule")
    @PreAuthorize("hasPermission('model:deploy')")
    @PostMapping("/api/v1/alert-rules")
    public R<AlertRuleVO> createAlertRule(@RequestBody @Valid AlertRuleCreateDTO dto) {
        return R.ok(alertService.createAlertRule(dto));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/api/v1/alert-rules")
    public R<List<AlertRuleVO>> listAlertRules(@RequestParam(required = false) Long deployment_id) {
        return R.ok(alertService.listAlertRules(deployment_id));
    }

    @OperLog(module = "alert", operation = "updateRule")
    @PreAuthorize("hasPermission('model:deploy')")
    @PutMapping("/api/v1/alert-rules/{id}")
    public R<AlertRuleVO> updateAlertRule(@PathVariable Long id, @RequestBody AlertRuleCreateDTO dto) {
        return R.ok(alertService.updateAlertRule(id, dto));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/api/v1/alerts")
    public R<PageResult<AlertRecordVO>> listAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        return R.ok(alertService.listAlerts(status, page, page_size));
    }

    @OperLog(module = "alert", operation = "acknowledge")
    @PreAuthorize("hasPermission('model:deploy')")
    @PutMapping("/api/v1/alerts/{id}/acknowledge")
    public R<AlertRecordVO> acknowledgeAlert(@PathVariable Long id,
                                               @RequestHeader("X-User-Id") Long userId) {
        return R.ok(alertService.acknowledgeAlert(id, userId));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/api/v1/alerts/history")
    public R<PageResult<AlertRecordVO>> getAlertHistory(
            @RequestParam Long rule_id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        return R.ok(alertService.getAlertHistory(rule_id, page, page_size));
    }
}
