package com.maidc.common.log.audit;

import com.maidc.common.log.model.OperationLogData;
import com.maidc.common.log.model.OperationLogEvent;
import com.maidc.common.log.trace.TraceIds;
import com.maidc.common.mq.model.MaidcMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationListener;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计事件统一转发器：把 @OperLog 切面发布的 OperationLogEvent 发送到审计服务，
 * 替代各服务手写的 listener（auth/model 曾各写一份，data/task/label/msg 则完全遗漏）。
 * payload 字段结构与既有审计消费契约（AuditLogConsumer）逐字段一致。
 * RabbitTemplate 通过 ObjectProvider 延迟解析，规避组件扫描装配顺序问题；
 * 可用 maidc.audit.forward.enabled=false 关闭。
 */
@Slf4j
public class OperationLogEventForwarder implements ApplicationListener<OperationLogEvent> {

    /** 与 maidc-audit AuditRabbitMqConfig 中的常量保持一致 */
    public static final String AUDIT_EXCHANGE = "maidc.audit";
    public static final String AUDIT_ROUTING_KEY = "audit.operation";

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final String serviceName;

    public OperationLogEventForwarder(ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
                                      String serviceName) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
        this.serviceName = serviceName;
    }

    @Override
    public void onApplicationEvent(OperationLogEvent event) {
        OperationLogData data = event.getLogData();
        try {
            RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
            if (rabbitTemplate == null) {
                log.debug("[审计日志] 无可用 RabbitTemplate，跳过转发: operation={}", data.getOperation());
                return;
            }

            if (data.getUsername() == null && "logout".equals(data.getOperation())) {
                data.setUsername(extractUsernameFromRequest());
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("traceId", data.getTraceId());
            payload.put("userId", data.getUserId());
            payload.put("username", data.getUsername());
            payload.put("orgId", data.getOrgId());
            payload.put("serviceName", data.getServiceName());
            payload.put("operation", data.getOperation());
            payload.put("resourceType", data.getResourceType());
            payload.put("resourceId", data.getResourceId());
            payload.put("resourceName", data.getResourceName());
            payload.put("requestMethod", data.getRequestMethod());
            payload.put("requestUrl", data.getRequestUrl());
            payload.put("requestParams", data.getRequestParams());
            payload.put("durationMs", data.getDurationMs());
            payload.put("status", data.getStatus());
            payload.put("errorMessage", data.getErrorMessage());
            payload.put("ipAddress", data.getIpAddress());
            payload.put("userAgent", data.getUserAgent());
            payload.put("createdAt", LocalDateTime.now().toString());

            MaidcMessage message = MaidcMessage.of("OPERATION_LOG", payload, serviceName);
            message.setTraceId(data.getTraceId() != null
                    ? data.getTraceId() : MDC.get(TraceIds.MDC_TRACE_ID));
            rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_ROUTING_KEY, message);
            log.debug("[审计日志] 已发送到MQ: serviceName={}, operation={}",
                    data.getServiceName(), data.getOperation());
        } catch (Exception e) {
            log.warn("[审计日志] MQ发送失败: serviceName={}, operation={}, error={}",
                    data.getServiceName(), data.getOperation(), e.getMessage());
        }
    }

    /**
     * 登出场景用户名可能既不在 DTO 也不在请求头，从 Authorization 的 JWT claim 中兜底提取。
     */
    private String extractUsernameFromRequest() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
            String token = authHeader.substring(7);
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            int idx = payload.indexOf("\"username\"");
            if (idx < 0) idx = payload.indexOf("\"sub\"");
            if (idx < 0) return null;
            int colon = payload.indexOf(':', idx);
            int startQuote = payload.indexOf('"', colon + 1);
            int endQuote = payload.indexOf('"', startQuote + 1);
            if (startQuote > 0 && endQuote > startQuote) {
                return payload.substring(startQuote + 1, endQuote);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
