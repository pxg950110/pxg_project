package com.maidc.msg.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MsgRabbitMqConfig {

    // Exchanges
    public static final String MSG_EXCHANGE = "maidc.msg";
    public static final String MODEL_EXCHANGE = "maidc.model";
    public static final String DLX_EXCHANGE = "maidc.dlx";

    // Queues
    public static final String ALERT_NOTIFY_QUEUE = "alert.notify";
    public static final String APPROVAL_NOTIFY_QUEUE = "approval.notify";
    public static final String SYSTEM_NOTIFY_QUEUE = "system.notify";
    public static final String DLQ_MSG = "dlq.msg";

    // Routing Keys
    public static final String ALERT_NOTIFY_KEY = "alert.notify";
    public static final String APPROVAL_NOTIFY_KEY = "approval.notify";
    public static final String SYSTEM_NOTIFY_KEY = "system.notify";

    @Bean
    public DirectExchange msgExchange() {
        return new DirectExchange(MSG_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue alertNotifyQueue() {
        return QueueBuilder.durable(ALERT_NOTIFY_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_MSG)
                .build();
    }

    @Bean
    public Queue approvalNotifyQueue() {
        return QueueBuilder.durable(APPROVAL_NOTIFY_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_MSG)
                .build();
    }

    @Bean
    public Queue systemNotifyQueue() {
        return QueueBuilder.durable(SYSTEM_NOTIFY_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_MSG)
                .build();
    }

    @Bean
    public Queue dlqMsg() {
        return QueueBuilder.durable(DLQ_MSG).build();
    }

    // Bindings for model exchange -> msg queues
    @Bean
    public Binding alertNotifyBinding() {
        return BindingBuilder.bind(alertNotifyQueue())
                .to(new DirectExchange(MODEL_EXCHANGE, true, false))
                .with(ALERT_NOTIFY_KEY);
    }

    @Bean
    public Binding approvalNotifyBinding() {
        return BindingBuilder.bind(approvalNotifyQueue())
                .to(new DirectExchange(MODEL_EXCHANGE, true, false))
                .with(APPROVAL_NOTIFY_KEY);
    }

    @Bean
    public Binding systemNotifyBinding(DirectExchange msgExchange) {
        return BindingBuilder.bind(systemNotifyQueue())
                .to(msgExchange)
                .with(SYSTEM_NOTIFY_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlqMsg())
                .to(dlxExchange())
                .with("#");
    }
}
