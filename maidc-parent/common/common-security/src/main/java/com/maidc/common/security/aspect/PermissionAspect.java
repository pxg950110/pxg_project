package com.maidc.common.security.aspect;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.security.annotation.RequirePermission;
import com.maidc.common.security.audit.PermissionAuditPublisher;
import com.maidc.common.security.context.CurrentUser;
import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.store.PermissionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** @RequirePermission 执行器：无用户=401，无权限=403（403 时 best-effort 发越权审计事件） */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionStore store;
    private final PermissionAuditPublisher auditPublisher;

    @Before("@annotation(rp)")
    public void before(JoinPoint jp, RequirePermission rp) {
        check(rp.value(), CurrentUser.userId());
    }

    /** 核心校验（切面与单测共用入口） */
    public void check(String code, Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "未认证");
        }
        PermissionContext ctx = store.load(userId);
        if (ctx == null || !ctx.has(code)) {
            log.warn("权限拒绝 userId={} code={}", userId, code);
            // 仅审计越权拒绝（403），不审计未认证（401）
            auditPublisher.publishDenied(userId, code, currentUri());
            throw new BusinessException(403, "无权限访问: " + code);
        }
    }

    private String currentUri() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra) {
            return sra.getRequest().getRequestURI();
        }
        return null;
    }
}
