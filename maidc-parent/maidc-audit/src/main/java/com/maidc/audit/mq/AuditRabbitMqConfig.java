package com.maidc.audit.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditRabbitMqConfig {

    public static final String AUDIT_EXCHANGE = "maidc.audit";
    public static final String AUDIT_QUEUE = "audit.operation";
    public static final String AUDIT_ROUTING_KEY = "audit.operation";
    public static final String DLX_EXCHANGE = "maidc.dlx";
    public static final String DLQ_AUDIT = "dlq.audit";
    /** 越权拒绝事件队列（routing key 同名，与 audit.operation 同款 durable + DLX 模式） */
    public static final String PERMISSION_DENIED_QUEUE = "audit.permission.denied";

    @Bean
    public DirectExchange auditExchange() {
        return new DirectExchange(AUDIT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange auditDlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue auditOperationQueue() {
        return QueueBuilder.durable(AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_AUDIT)
                .build();
    }

    @Bean
    public Queue dlqAudit() {
        return QueueBuilder.durable(DLQ_AUDIT).build();
    }

    @Bean
    public Binding auditBinding() {
        return BindingBuilder.bind(auditOperationQueue()).to(auditExchange()).with(AUDIT_ROUTING_KEY);
    }

    @Bean
    public Queue permissionDeniedQueue() {
        return QueueBuilder.durable(PERMISSION_DENIED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_AUDIT)
                .build();
    }

    @Bean
    public Binding permissionDeniedBinding() {
        return BindingBuilder.bind(permissionDeniedQueue()).to(auditExchange()).with(PERMISSION_DENIED_QUEUE);
    }

    @Bean
    public Binding dlqAuditBinding() {
        return BindingBuilder.bind(dlqAudit()).to(auditDlxExchange()).with(DLQ_AUDIT);
    }
}
