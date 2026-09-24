package com.maidc.common.security.audit;

import com.maidc.common.mq.model.MaidcMessage;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * 权限拒绝/越权事件发布（best-effort）：
 * 未接 MQ 的服务没有 RabbitTemplate Bean 时仅记日志；
 * 发送失败仅告警——两种情况均不影响主流程响应码。
 */
@Slf4j
@Component
public class PermissionAuditPublisher {

    /** 与 maidc-audit AuditRabbitMqConfig 的 AUDIT_EXCHANGE 对应 */
    public static final String EXCHANGE = "maidc.audit";
    public static final String ROUTING_KEY = "audit.permission.denied";
    public static final String EVENT_TYPE = "PERMISSION_DENIED";
    public static final String SOURCE = "common-security";

    /** required=false：无 MQ 环境下保持可注入，publishDenied 静默降级 */
    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    public void publishDenied(Long userId, String code, String uri) {
        if (rabbitTemplate == null) {
            log.info("[audit-deny] userId={} code={} uri={} (mq unavailable)", userId, code, uri);
            return;
        }
        // HashMap 允许 null 值（Map.of 遇 null 直接 NPE，单测/无请求上下文时 uri 为 null）
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("permissionCode", code);
        payload.put("uri", uri);
        payload.put("orgId", currentOrgId());
        try {
            MaidcMessage msg = MaidcMessage.of(EVENT_TYPE, payload, SOURCE);
            String traceId = MDC.get("traceId");
            if (traceId != null) {
                msg.setTraceId(traceId);
            }
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, msg);
        } catch (Exception e) {
            log.warn("权限拒绝事件发布失败 userId={} code={}", userId, code, e);
        }
    }

    /** 网关注入的 X-Org-Id 头（审计表 org_id 非空，尽量带上；取不到返回 null，消费侧兜底） */
    private Long currentOrgId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra) {
            String v = sra.getRequest().getHeader("X-Org-Id");
            if (v != null && !v.isBlank()) {
                try {
                    return Long.valueOf(v);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }
}
