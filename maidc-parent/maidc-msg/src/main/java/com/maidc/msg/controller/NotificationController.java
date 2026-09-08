package com.maidc.msg.controller;

import com.maidc.common.core.result.PageResult;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 分页查询用户通知列表
     */
    @PreAuthorize("hasPermission('msg:read')")
    @GetMapping
    public R<PageResult<Map<String, Object>>> listNotifications(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setItems(Collections.emptyList());
        result.setTotal(0L);
        result.setPage(page);
        result.setPageSize(page_size);
        result.setTotalPages(0);
        return R.ok(result);
    }

    /**
     * 获取用户通知设置列表
     */
    @PreAuthorize("hasPermission('msg:read')")
    @GetMapping("/settings")
    public R<List<NotificationSettingVO>> getSettings(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return R.ok(notificationService.getSettings(userId != null ? userId : 1L));
    }

    /**
     * 更新用户通知设置
     */
    @OperLog(module = "notification", operation = "updateSetting")
    @PreAuthorize("hasPermission('msg:setting')")
    @PutMapping("/settings/{id}")
    public R<NotificationSettingVO> updateSetting(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id,
            @RequestBody @Valid NotificationSettingDTO dto) {
        return R.ok(notificationService.updateSetting(userId != null ? userId : 1L, id, dto));
    }

    /**
     * 创建用户通知设置
     */
    @OperLog(module = "notification", operation = "createSetting")
    @PreAuthorize("hasPermission('msg:setting')")
    @PostMapping("/settings")
    public R<NotificationSettingVO> createSetting(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody @Valid NotificationSettingDTO dto) {
        return R.ok(notificationService.createSetting(userId != null ? userId : 1L, dto));
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
