package com.maidc.audit.mq;

import com.maidc.audit.entity.AuditLogEntity;
import com.maidc.audit.repository.AuditLogRepository;
import com.maidc.common.mq.consumer.BaseMessageConsumer;
import com.maidc.common.mq.model.MaidcMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogConsumer extends BaseMessageConsumer {

    private final AuditLogRepository auditLogRepository;

    @RabbitListener(queues = "audit.operation")
    public void onAuditOperation(MaidcMessage message) {
        handleMessage(message);
    }

    @Override
    protected void processMessage(MaidcMessage message) {
        Map<String, Object> payload = message.getPayload();

        AuditLogEntity entity = new AuditLogEntity();
        // trace_id 列 NOT NULL：MDC 中已有消费入口还原/兜底的 traceId（含格式规范化），
        // 消息缺 traceId 时告警并落兜底值，避免过渡期消息违约进 DLQ
        String payloadTraceId = extractString(payload, "traceId");
        if (payloadTraceId == null || payloadTraceId.isBlank()) {
            log.warn("[审计日志] 消息缺少 traceId，落兜底值: serviceName={}, operation={}",
                    extractString(payload, "serviceName"), extractString(payload, "operation"));
        }
        entity.setTraceId(MDC.get("traceId"));
        entity.setUserId(extractLong(payload, "userId"));
        entity.setUsername(extractString(payload, "username"));
        entity.setServiceName(extractString(payload, "serviceName"));
        entity.setOperation(extractString(payload, "operation"));
        entity.setResourceType(extractString(payload, "resourceType"));
        entity.setResourceId(extractString(payload, "resourceId"));
        entity.setResourceName(extractString(payload, "resourceName"));
        entity.setRequestMethod(extractString(payload, "requestMethod"));
        entity.setRequestUrl(extractString(payload, "requestUrl"));
        entity.setRequestParams(extractString(payload, "requestParams"));
        entity.setResponseCode(extractInteger(payload, "responseCode"));
        entity.setResponseMsg(extractString(payload, "responseMsg"));
        entity.setIpAddress(extractString(payload, "ipAddress"));
        entity.setUserAgent(extractString(payload, "userAgent"));
        entity.setDurationMs(extractInteger(payload, "durationMs"));
        entity.setStatus(extractString(payload, "status"));
        entity.setErrorMessage(extractString(payload, "errorMessage"));
        // org_id 列 NOT NULL：未认证请求（如登录）无用户头，payload 缺失时兜底 0（与 PermissionDeniedConsumer 一致）
        Long orgId = extractLong(payload, "orgId");
        entity.setOrgId(orgId != null ? orgId : 0L);

        String createdAtStr = extractString(payload, "createdAt");
        entity.setCreatedAt(createdAtStr != null ? LocalDateTime.parse(createdAtStr) : LocalDateTime.now());

        auditLogRepository.save(entity);
        log.info("[审计日志] 已持久化: serviceName={}, operation={}, username={}",
                entity.getServiceName(), entity.getOperation(), entity.getUsername());
    }

    private String extractString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }

    private Long extractLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer extractInteger(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
