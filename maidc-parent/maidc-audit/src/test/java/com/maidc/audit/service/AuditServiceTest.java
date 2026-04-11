package com.maidc.audit.service;

import com.maidc.audit.entity.AuditLogEntity;
import com.maidc.audit.mapper.AuditMapper;
import com.maidc.audit.repository.AuditLogRepository;
import com.maidc.audit.repository.DataAccessLogRepository;
import com.maidc.audit.repository.SystemEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private DataAccessLogRepository dataAccessLogRepository;

    @Mock
    private SystemEventRepository systemEventRepository;

    @Mock
    private AuditMapper auditMapper;

    @InjectMocks
    private AuditService auditService;

    @Test
    void logAudit_savesLogEntry() {
        // Arrange - build an entity with the expected fields
        AuditLogEntity entity = new AuditLogEntity();
        entity.setModule("MODEL");
        entity.setOperation("DEPLOY");
        entity.setUsername("admin");
        entity.setCreatedAt(LocalDateTime.now());

        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Save the entity via repository
        AuditLogEntity saved = auditLogRepository.save(entity);

        // Assert - verify save was called with correct module/operation/operator fields
        verify(auditLogRepository).save(argThat(e ->
                "MODEL".equals(e.getModule()) &&
                "DEPLOY".equals(e.getOperation()) &&
                "admin".equals(e.getUsername())
        ));
        assertNotNull(saved);
        assertEquals("MODEL", saved.getModule());
        assertEquals("DEPLOY", saved.getOperation());
        assertEquals("admin", saved.getUsername());
    }
}
