package com.maidc.common.mq.producer;

import com.maidc.common.mq.model.MaidcMessage;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class BaseMessageProducer {

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    protected void send(String exchange, String routingKey, MaidcMessage message) {
        if (message.getTraceId() == null) {
            message.setTraceId(MDC.get("traceId"));
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(java.time.LocalDateTime.now());
        }
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        log.info("消息发送: exchange={}, routingKey={}, eventType={}",
                exchange, routingKey, message.getEventType());
    }
}
