package com.maidc.msg.mq;

import com.maidc.common.mq.consumer.BaseMessageConsumer;
import com.maidc.common.mq.model.MaidcMessage;
import com.maidc.msg.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertNotifyConsumer extends BaseMessageConsumer {

    private final MessageService messageService;

    /**
     * 监听告警通知队列，将告警事件转化为站内消息
     */
    @RabbitListener(queues = "alert.notify")
    public void onAlertNotify(MaidcMessage message) {
        handleMessage(message);
    }

    /**
     * 监听审批通知队列
     */
    @RabbitListener(queues = "approval.notify")
    public void onApprovalNotify(MaidcMessage message) {
        handleMessage(message);
    }

    /**
     * 监听系统通知队列
     */
    @RabbitListener(queues = "system.notify")
    public void onSystemNotify(MaidcMessage message) {
        handleMessage(message);
    }

    @Override
    protected void processMessage(MaidcMessage message) {
        Map<String, Object> payload = message.getPayload();
        String eventType = message.getEventType();

        Long userId = extractLong(payload, "userId");
        String title = extractString(payload, "title");
        String content = extractString(payload, "content");
        Long bizId = extractLong(payload, "bizId");
        String bizType = extractString(payload, "bizType");

        // 根据事件类型确定消息类型
        String messageType = resolveMessageType(eventType);

        if (userId != null && title != null) {
            messageService.sendMessage(userId, title, content, messageType, bizId, bizType);
            log.info("告警通知已转化为站内消息: userId={}, eventType={}", userId, eventType);
        } else {
            log.warn("告警通知缺少必要字段，跳过处理: userId={}, title={}", userId, title);
        }
    }

    private String resolveMessageType(String eventType) {
        if (eventType == null) {
            return "SYSTEM";
        }
        return switch (eventType) {
            case "alert.triggered", "alert.acknowledged", "alert.resolved" -> "ALERT";
            case "approval.created", "approval.approved", "approval.rejected" -> "APPROVAL";
            default -> "SYSTEM";
        };
    }

    private Long extractLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }
}
