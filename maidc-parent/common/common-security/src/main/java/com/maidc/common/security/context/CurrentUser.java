package com.maidc.common.security.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 从请求头(X-User-Id, 网关注入)取当前用户，兜底 SecurityContext */
public final class CurrentUser {

    private CurrentUser() {}

    public static Long userId() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            String v = sra.getRequest().getHeader("X-User-Id");
            if (v != null && !v.isBlank()) {
                try { return Long.valueOf(v); } catch (NumberFormatException ignored) {}
            }
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getDetails() instanceof Long id) {
            return id;
        }
        return null;
    }
}
