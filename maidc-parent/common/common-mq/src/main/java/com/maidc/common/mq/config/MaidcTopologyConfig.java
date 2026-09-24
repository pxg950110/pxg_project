package com.maidc.common.mq.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 跨服务共享的 MQ 拓扑声明（RabbitAdmin 启动时幂等声明）。
 * 仅声明没有归属配置类的遗留队列；已在各服务 RabbitMqConfig 中声明的队列
 * 不得在此重复定义，参数不一致会触发 broker 的 PRECONDITION_FAILED。
 */
@Configuration
public class MaidcTopologyConfig {

    /** 标注结果通知队列：maidc-data / maidc-task 的 PersonalTaskConsumer 消费，历史上无声明方 */
    public static final String LABEL_NOTIFY_QUEUE = "label.notify";

    @Bean
    public Queue labelNotifyQueue() {
        return QueueBuilder.durable(LABEL_NOTIFY_QUEUE).build();
    }
}
