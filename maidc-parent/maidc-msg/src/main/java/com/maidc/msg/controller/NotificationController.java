package com.maidc.msg.controller;

import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.msg.dto.NotificationSettingDTO;
import com.maidc.msg.dto.TemplateCreateDTO;
import com.maidc.msg.service.NotificationService;
import com.maidc.msg.vo.NotificationSettingVO;
import com.maidc.msg.vo.TemplateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取用户通知设置列表
     */
    @PreAuthorize("hasPermission('msg:read')")
    @GetMapping("/settings")
    public R<List<NotificationSettingVO>> getSettings(@RequestHeader("X-User-Id") Long userId) {
        return R.ok(notificationService.getSettings(userId));
    }

    /**
     * 更新用户通知设置
     */
    @OperLog(module = "notification", operation = "updateSetting")
    @PreAuthorize("hasPermission('msg:setting')")
    @PutMapping("/settings/{id}")
    public R<NotificationSettingVO> updateSetting(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @RequestBody @Valid NotificationSettingDTO dto) {
        return R.ok(notificationService.updateSetting(userId, id, dto));
    }

    /**
     * 创建用户通知设置
     */
    @OperLog(module = "notification", operation = "createSetting")
    @PreAuthorize("hasPermission('msg:setting')")
    @PostMapping("/settings")
    public R<NotificationSettingVO> createSetting(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid NotificationSettingDTO dto) {
        return R.ok(notificationService.createSetting(userId, dto));
    }

    /**
     * 获取所有消息模板
     */
    @PreAuthorize("hasPermission('msg:template')")
    @GetMapping("/templates")
    public R<List<TemplateVO>> getTemplates() {
        return R.ok(notificationService.getTemplates());
    }

    /**
     * 创建消息模板
     */
    @OperLog(module = "notification", operation = "createTemplate")
    @PreAuthorize("hasPermission('msg:template')")
    @PostMapping("/templates")
    public R<TemplateVO> createTemplate(@RequestBody @Valid TemplateCreateDTO dto) {
        return R.ok(notificationService.createTemplate(dto));
    }

    /**
     * 更新消息模板
     */
    @OperLog(module = "notification", operation = "updateTemplate")
    @PreAuthorize("hasPermission('msg:template')")
    @PutMapping("/templates/{id}")
    public R<TemplateVO> updateTemplate(
            @PathVariable Long id,
            @RequestBody @Valid TemplateCreateDTO dto) {
        return R.ok(notificationService.updateTemplate(id, dto));
    }
}
