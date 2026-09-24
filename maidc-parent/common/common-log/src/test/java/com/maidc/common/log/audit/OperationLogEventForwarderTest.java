package com.maidc.common.log.audit;

import com.maidc.common.log.model.OperationLogData;
import com.maidc.common.log.model.OperationLogEvent;
import com.maidc.common.mq.model.MaidcMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OperationLogEventForwarderTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldSendPayloadMatchingAuditConsumerContract() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ObjectProvider<RabbitTemplate> provider = mock(ObjectProvider.class);
        org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(rabbitTemplate);

        OperationLogEventForwarder forwarder = new OperationLogEventForwarder(provider, "maidc-data");

        OperationLogData data = OperationLogData.builder()
                .serviceName("数据服务")
                .operation("upload")
                .traceId("abc")
                .userId(1L)
                .username("alice")
                .orgId(2L)
                .status("SUCCESS")
                .durationMs(12)
                .build();
        forwarder.onApplicationEvent(new OperationLogEvent(this, data));

        ArgumentCaptor<MaidcMessage> captor = ArgumentCaptor.forClass(MaidcMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(OperationLogEventForwarder.AUDIT_EXCHANGE),
                eq(OperationLogEventForwarder.AUDIT_ROUTING_KEY),
                captor.capture());

        MaidcMessage message = captor.getValue();
        assertThat(message.getEventType()).isEqualTo("OPERATION_LOG");
        assertThat(message.getSource()).isEqualTo("maidc-data");
        assertThat(message.getTraceId()).isEqualTo("abc");

        List<String> contractKeys = List.of(
                "traceId", "userId", "username", "orgId", "serviceName", "operation",
                "resourceType", "resourceId", "resourceName", "requestMethod", "requestUrl",
                "requestParams", "durationMs", "status", "errorMessage", "ipAddress",
                "userAgent", "createdAt");
        contractKeys.forEach(key ->
                assertThat(message.getPayload()).containsKey(key));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSkipSilentlyWhenNoRabbitTemplate() {
        ObjectProvider<RabbitTemplate> provider = mock(ObjectProvider.class);
        org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(null);

        OperationLogEventForwarder forwarder = new OperationLogEventForwarder(provider, "maidc-data");
        OperationLogData data = OperationLogData.builder().operation("x").build();

        // 不应抛出异常
        forwarder.onApplicationEvent(new OperationLogEvent(this, data));
    }
}
