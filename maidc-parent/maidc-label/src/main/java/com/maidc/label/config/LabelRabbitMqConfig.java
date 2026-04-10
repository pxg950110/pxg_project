package com.maidc.label.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LabelRabbitMqConfig {

    // Exchanges
    public static final String LABEL_EXCHANGE = "maidc.label";
    public static final String DLX_EXCHANGE = "maidc.dlx";

    // Queues
    public static final String PREPROCESSING_QUEUE = "label.preprocessing";
    public static final String LABEL_RESULT_QUEUE = "label.result";
    public static final String DLQ_LABEL = "dlq.label";

    // Routing Keys
    public static final String PREPROCESSING_ROUTING_KEY = "label.preprocessing";
    public static final String LABEL_RESULT_ROUTING_KEY = "label.result";

    @Bean
    public DirectExchange labelExchange() {
        return new DirectExchange(LABEL_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue preprocessingQueue() {
        return QueueBuilder.durable(PREPROCESSING_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_LABEL)
                .build();
    }

    @Bean
    public Queue labelResultQueue() {
        return QueueBuilder.durable(LABEL_RESULT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_LABEL)
                .build();
    }

    @Bean
    public Queue dlqLabel() {
        return QueueBuilder.durable(DLQ_LABEL).build();
    }

    @Bean
    public Binding preprocessingBinding(DirectExchange labelExchange) {
        return BindingBuilder.bind(preprocessingQueue())
                .to(labelExchange)
                .with(PREPROCESSING_ROUTING_KEY);
    }

    @Bean
    public Binding labelResultBinding(DirectExchange labelExchange) {
        return BindingBuilder.bind(labelResultQueue())
                .to(labelExchange)
                .with(LABEL_RESULT_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlqLabel())
                .to(dlxExchange())
                .with("#");
    }
}
