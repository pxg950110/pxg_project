package com.maidc.common.log.config;

import com.maidc.common.log.aspect.OperationLogAspect;
import com.maidc.common.log.audit.OperationLogEventForwarder;
import com.maidc.common.log.filter.TraceContextFilter;
import com.maidc.common.log.web.TraceRestTemplateCustomizer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 本类通过两种机制均可生效：各服务 @SpringBootApplication 扫描 com.maidc.common
 * （组件扫描，优先），以及 META-INF/spring/...AutoConfiguration.imports（Boot 3 规范，
 * 供不扫描 com.maidc.common 的应用使用）。条件装配用 ObjectProvider 而非
 * @ConditionalOnBean，避免组件扫描场景下的注册顺序不确定性。
 */
@Configuration
public class OperationLogAutoConfiguration {

    @Bean
    public OperationLogAspect operationLogAspect(ApplicationEventPublisher eventPublisher) {
        OperationLogAspect aspect = new OperationLogAspect();
        aspect.setEventPublisher(eventPublisher);
        return aspect;
    }

    @Bean
    public TraceContextFilter traceContextFilter() {
        return new TraceContextFilter();
    }

    @Bean
    public TraceRestTemplateCustomizer traceRestTemplateCustomizer() {
        return new TraceRestTemplateCustomizer();
    }

    @Bean
    @ConditionalOnProperty(name = "maidc.audit.forward.enabled", havingValue = "true", matchIfMissing = true)
    public OperationLogEventForwarder operationLogEventForwarder(
            ObjectProvider<RabbitTemplate> rabbitTemplateProvider, Environment environment) {
        return new OperationLogEventForwarder(rabbitTemplateProvider,
                environment.getProperty("spring.application.name", "unknown"));
    }
}
