package com.maidc.audit.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.audit.entity.SystemEventEntity;
import com.maidc.audit.repository.SystemEventRepository;
import com.maidc.common.mq.consumer.BaseMessageConsumer;
import com.maidc.common.mq.model.MaidcMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 越权拒绝事件消费：PERMISSION_DENIED → audit.a_system_event 落库。
 *
 * event_type CHECK 约束已由 docker/init-db/18-audit-event-type.sql 扩展，
 * 原生 PERMISSION_DENIED 类型合法（语义保真，审计可按类型检索）；
 * event_level 用 WARN（约束原生允许），完整 payload 保留在 event_data(JSONB)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionDeniedConsumer extends BaseMessageConsumer {

    /** a_system_event.event_type CHECK 允许值（18-audit-event-type.sql 已扩展） */
    static final String DB_EVENT_TYPE = "PERMISSION_DENIED";
    /** a_system_event.event_level CHECK 允许值 */
    static final String DB_EVENT_LEVEL = "WARN";
    /** org_id 列 NOT NULL，payload 缺失时兜底 0（未知机构） */
    static final long DEFAULT_ORG_ID = 0L;

    private final SystemEventRepository systemEventRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = AuditRabbitMqConfig.PERMISSION_DENIED_QUEUE)
    public void onPermissionDenied(MaidcMessage message) {
        handleMessage(message);
    }

    @Override
    protected void processMessage(MaidcMessage message) {
        Map<String, Object> payload = message.getPayload();
        Long userId = extractLong(payload, "userId");
        String permissionCode = extractString(payload, "permissionCode");
        String uri = extractString(payload, "uri");

        SystemEventEntity entity = new SystemEventEntity();
        entity.setEventType(DB_EVENT_TYPE);
        entity.setEventLevel(DB_EVENT_LEVEL);
        entity.setSource(message.getSource() != null ? message.getSource() : "common-security");
        entity.setEventTitle("越权访问被拒绝");
        entity.setEventDetail(String.format("用户[%s]尝试访问无权限资源 permission=%s uri=%s",
                userId, permissionCode, uri));
        entity.setEventData(toJson(message));
        entity.setTraceId(message.getTraceId());
        entity.setResolved(false);
        Long orgId = extractLong(payload, "orgId");
        entity.setOrgId(orgId != null ? orgId : DEFAULT_ORG_ID);
        entity.setCreatedAt(LocalDateTime.now());

        systemEventRepository.save(entity);
        log.info("[系统事件] 越权拒绝已落库: userId={} permission={} uri={}", userId, permissionCode, uri);
    }

    /** 原始事件类型 + 完整 payload 序列化为 JSONB（连接串 stringtype=unspecified，String 直写） */
    private String toJson(MaidcMessage message) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventType", message.getEventType());
        data.put("payload", message.getPayload());
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("event_data 序列化失败，退化为 toString: {}", e.getMessage());
            return String.valueOf(data);
        }
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
}
