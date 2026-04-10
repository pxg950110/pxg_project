package com.maidc.common.mq.consumer;

import com.maidc.common.mq.model.MaidcMessage;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

@Slf4j
public abstract class BaseMessageConsumer {

    protected void handleMessage(MaidcMessage message) {
        String traceId = message.getTraceId();
        if (traceId != null) {
            MDC.put("traceId", traceId);
        }
        try {
            log.info("消息接收: eventType={}, source={}", message.getEventType(), message.getSource());
            processMessage(message);
        } catch (Exception e) {
            log.error("消息处理失败: eventType={}, error={}", message.getEventType(), e.getMessage(), e);
            throw new AmqpRejectAndDontRequeueException("处理失败，转入死信队列", e);
        } finally {
            MDC.remove("traceId");
        }
    }

    protected abstract void processMessage(MaidcMessage message);
}
