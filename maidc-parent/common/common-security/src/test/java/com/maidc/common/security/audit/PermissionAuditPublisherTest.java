package com.maidc.common.security.audit;

import com.maidc.common.mq.model.MaidcMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PermissionAuditPublisherTest {

    @Test
    void silentlySkips_whenNoRabbitTemplateBean() {
        PermissionAuditPublisher publisher = new PermissionAuditPublisher(); // rabbitTemplate 保持 null
        assertDoesNotThrow(() -> publisher.publishDenied(1L, "cdr:patient:read", null));
    }

    @Test
    void sendsToAuditExchange_withNullSafePayload() {
        PermissionAuditPublisher publisher = new PermissionAuditPublisher();
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ReflectionTestUtils.setField(publisher, "rabbitTemplate", rabbitTemplate);

        publisher.publishDenied(1L, "cdr:patient:read", null);

        ArgumentCaptor<MaidcMessage> captor = ArgumentCaptor.forClass(MaidcMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(PermissionAuditPublisher.EXCHANGE),
                eq(PermissionAuditPublisher.ROUTING_KEY),
                captor.capture());
        MaidcMessage msg = captor.getValue();
        assertEquals("PERMISSION_DENIED", msg.getEventType());
        assertEquals("common-security", msg.getSource());
        assertEquals(1L, msg.getPayload().get("userId"));
        assertEquals("cdr:patient:read", msg.getPayload().get("permissionCode"));
        // URI 为 null 时不得抛 NPE（Map.of 不允许 null）
        assertNull(msg.getPayload().get("uri"));
    }

    @Test
    void swallowsSendFailure_bestEffort() {
        PermissionAuditPublisher publisher = new PermissionAuditPublisher();
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ReflectionTestUtils.setField(publisher, "rabbitTemplate", rabbitTemplate);
        doThrow(new IllegalStateException("broker down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> publisher.publishDenied(1L, "cdr:patient:read", "/api/cdr/patients"));
    }
}
