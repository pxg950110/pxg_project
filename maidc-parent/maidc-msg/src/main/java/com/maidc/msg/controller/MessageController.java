package com.maidc.msg.controller;

import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.msg.service.MessageService;
import com.maidc.msg.vo.MessageVO;
import com.maidc.msg.vo.UnreadCountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;

    /**
     * 分页查询用户消息列表
     */
    @PreAuthorize("hasPermission('msg:read')")
    @GetMapping
    public R<PageResult<MessageVO>> listMessages(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean is_read,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        return R.ok(messageService.listMessages(userId, type, is_read, page, page_size));
    }

    /**
     * 标记单条消息为已读
     */
    @OperLog(module = "message", operation = "markAsRead")
    @PreAuthorize("hasPermission('msg:read')")
    @PutMapping("/{id}/read")
    public R<Void> markAsRead(@PathVariable Long id) {
        messageService.markAsRead(id);
        return R.ok();
    }

    /**
     * 标记所有消息为已读
     */
    @OperLog(module = "message", operation = "markAllAsRead")
    @PreAuthorize("hasPermission('msg:read')")
    @PutMapping("/read-all")
    public R<Void> markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        messageService.markAllAsRead(userId);
        return R.ok();
    }

    /**
     * 获取未读消息数
     */
    @PreAuthorize("hasPermission('msg:read')")
    @GetMapping("/unread-count")
    public R<UnreadCountVO> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        return R.ok(messageService.getUnreadCount(userId));
    }
}
