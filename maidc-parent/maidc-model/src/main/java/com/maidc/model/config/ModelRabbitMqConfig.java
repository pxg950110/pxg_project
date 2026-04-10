package com.maidc.model.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelRabbitMqConfig {

    // Exchanges
    public static final String MODEL_EXCHANGE = "maidc.model";
    public static final String DLX_EXCHANGE = "maidc.dlx";

    // Queues
    public static final String EVALUATION_QUEUE = "model.evaluation";
    public static final String EVALUATION_RESULT_QUEUE = "model.evaluation.result";
    public static final String DEPLOYMENT_QUEUE = "model.deployment";
    public static final String BATCH_INFERENCE_QUEUE = "model.inference.batch";
    public static final String DLQ_MODEL = "dlq.model";

    // Routing Keys
    public static final String EVALUATION_KEY = "evaluation";
    public static final String EVALUATION_RESULT_KEY = "evaluation.result";
    public static final String DEPLOYMENT_KEY = "deployment";
    public static final String BATCH_INFERENCE_KEY = "inference.batch";

    @Bean
    public DirectExchange modelExchange() {
        return new DirectExchange(MODEL_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue evaluationQueue() {
        return QueueBuilder.durable(EVALUATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_MODEL)
                .build();
    }

    @Bean
    public Queue evaluationResultQueue() {
        return QueueBuilder.durable(EVALUATION_RESULT_QUEUE).build();
    }

    @Bean
    public Queue deploymentQueue() {
        return QueueBuilder.durable(DEPLOYMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_MODEL)
                .build();
    }

    @Bean
    public Queue batchInferenceQueue() {
        return QueueBuilder.durable(BATCH_INFERENCE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Queue dlqModel() {
        return QueueBuilder.durable(DLQ_MODEL).build();
    }

    @Bean
    public Binding evaluationBinding() {
        return BindingBuilder.bind(evaluationQueue()).to(modelExchange()).with(EVALUATION_KEY);
    }

    @Bean
    public Binding evaluationResultBinding() {
        return BindingBuilder.bind(evaluationResultQueue()).to(modelExchange()).with(EVALUATION_RESULT_KEY);
    }

    @Bean
    public Binding deploymentBinding() {
        return BindingBuilder.bind(deploymentQueue()).to(modelExchange()).with(DEPLOYMENT_KEY);
    }

    @Bean
    public Binding batchInferenceBinding() {
        return BindingBuilder.bind(batchInferenceQueue()).to(modelExchange()).with(BATCH_INFERENCE_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlqModel()).to(dlxExchange()).with("#");
    }
}
