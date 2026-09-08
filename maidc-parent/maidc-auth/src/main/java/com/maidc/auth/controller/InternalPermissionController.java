package com.maidc.auth.controller;

import com.maidc.auth.service.PermissionCacheService;
import com.maidc.common.security.annotation.PublicEndpoint;
import com.maidc.common.security.context.PermissionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务间内部端点：权限缓存 miss 时由各业务服务的 PermissionStore 调用重建。
 * 直连 auth 服务端口（不经网关），SecurityConfig 仅放行本机/内网来源。
 * <p>必须标注 @PublicEndpoint（而非 @RequirePermission）：
 * Redis miss 时懒加载若再走权限切面，会对自身发起 HTTP 调用形成无限递归。
 */
@RestController
@RequestMapping("/api/v1/internal/permissions")
@RequiredArgsConstructor
public class InternalPermissionController {

    private final PermissionCacheService cacheService;

    @PublicEndpoint(reason = "service-to-service lazy load")
    @GetMapping("/{userId}")
    public PermissionContext get(@PathVariable Long userId) {
        return cacheService.build(userId);
    }
}
