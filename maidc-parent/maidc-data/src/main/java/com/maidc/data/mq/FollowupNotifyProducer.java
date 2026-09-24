package com.maidc.data.mq;

import com.maidc.common.mq.model.MaidcMessage;
import com.maidc.common.mq.producer.BaseMessageProducer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 随访提醒消息：走 maidc.msg exchange / system.notify 队列（队列由 maidc-msg 声明并消费），
 * 由 maidc-msg AlertNotifyConsumer 转为站内信。exchange/routing key 与 msg 端常量保持一致。
 */
@Component
public class FollowupNotifyProducer extends BaseMessageProducer {

    public static final String MSG_EXCHANGE = "maidc.msg";
    public static final String SYSTEM_NOTIFY_KEY = "system.notify";

    public void sendFollowupNotify(String eventType, Map<String, Object> payload) {
        send(MSG_EXCHANGE, SYSTEM_NOTIFY_KEY, MaidcMessage.of(eventType, payload, "maidc-data"));
    }
}
