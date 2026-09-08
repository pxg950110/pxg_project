package com.maidc.audit.mq;

import com.maidc.audit.entity.SystemEventEntity;
import com.maidc.audit.repository.SystemEventRepository;
import com.maidc.common.mq.model.MaidcMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PermissionDeniedConsumerTest {

    private final SystemEventRepository systemEventRepository = mock(SystemEventRepository.class);
    private final PermissionDeniedConsumer consumer =
            new PermissionDeniedConsumer(systemEventRepository, new ObjectMapper());

    @Test
    void persistsSystemEvent_withCheckCompatibleTypeAndLevel() {
        when(systemEventRepository.save(any(SystemEventEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1L);
        payload.put("permissionCode", "cdr:patient:read");
        payload.put("uri", "/api/cdr/patients/1");
        payload.put("orgId", null);
        consumer.onPermissionDenied(MaidcMessage.of("PERMISSION_DENIED", payload, "common-security"));

        ArgumentCaptor<SystemEventEntity> captor = ArgumentCaptor.forClass(SystemEventEntity.class);
        verify(systemEventRepository).save(captor.capture());
        SystemEventEntity saved = captor.getValue();
        // event_type CHECK 约束已由 18-audit-event-type.sql 扩展，原生类型落库
        assertEquals("PERMISSION_DENIED", saved.getEventType());
        assertEquals("WARN", saved.getEventLevel());
        assertEquals("common-security", saved.getSource());
        assertNotNull(saved.getEventTitle());
        assertNotNull(saved.getCreatedAt());
        // org_id 列 NOT NULL，缺失时兜底 0
        assertEquals(0L, saved.getOrgId());
        // 完整 payload 保留在 event_data JSONB
        assertTrue(saved.getEventData().contains("PERMISSION_DENIED"));
        assertTrue(saved.getEventData().contains("cdr:patient:read"));
    }
}
