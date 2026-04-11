package com.maidc.audit.service;

import com.maidc.audit.dto.AuditLogQueryDTO;
import com.maidc.audit.entity.AuditLogEntity;
import com.maidc.audit.mapper.AuditMapper;
import com.maidc.audit.repository.AuditLogRepository;
import com.maidc.audit.repository.DataAccessLogRepository;
import com.maidc.audit.repository.SystemEventRepository;
import com.maidc.audit.vo.AuditLogVO;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditQueryTest {

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
    void queryAuditLogs_withFilters_returnsPageResult() {
        // Arrange
        AuditLogQueryDTO queryDTO = new AuditLogQueryDTO();
        queryDTO.setModule("data");
        queryDTO.setOperation("export");
        queryDTO.setUsername("admin");
        queryDTO.setPage(1);
        queryDTO.setPageSize(10);

        AuditLogEntity entity = new AuditLogEntity();
        entity.setId("audit-001");
        entity.setModule("data");
        entity.setOperation("export");
        entity.setUsername("admin");
        entity.setStatus((short) 1);

        AuditLogVO vo = new AuditLogVO();
        vo.setId("audit-001");
        vo.setModule("data");
        vo.setOperation("export");
        vo.setUsername("admin");

        Page<AuditLogEntity> mockPage = new PageImpl<>(List.of(entity));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(auditMapper.toAuditLogVO(entity)).thenReturn(vo);

        // Act
        PageResult<AuditLogVO> result = auditService.queryAuditLogs(queryDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getItems().size());
        assertEquals("audit-001", result.getItems().get(0).getId());
        verify(auditLogRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void queryAuditLogs_withDateRange_returnsFilteredResults() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 12, 31, 23, 59);

        AuditLogQueryDTO queryDTO = new AuditLogQueryDTO();
        queryDTO.setStartTime(startTime);
        queryDTO.setEndTime(endTime);
        queryDTO.setStatus((short) 0);
        queryDTO.setPage(1);
        queryDTO.setPageSize(20);

        Page<AuditLogEntity> emptyPage = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PageResult<AuditLogVO> result = auditService.queryAuditLogs(queryDTO);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getItems().isEmpty());
        verify(auditLogRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAuditLogDetail_existingId_returnsVO() {
        // Arrange
        String auditId = "audit-123";
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(auditId);
        entity.setModule("model");
        entity.setOperation("deploy");
        entity.setUsername("engineer");

        AuditLogVO expectedVO = new AuditLogVO();
        expectedVO.setId(auditId);
        expectedVO.setModule("model");
        expectedVO.setOperation("deploy");
        expectedVO.setUsername("engineer");

        when(auditLogRepository.findById(auditId)).thenReturn(Optional.of(entity));
        when(auditMapper.toAuditLogVO(entity)).thenReturn(expectedVO);

        // Act
        AuditLogVO result = auditService.getAuditLogDetail(auditId);

        // Assert
        assertNotNull(result);
        assertEquals(auditId, result.getId());
        assertEquals("model", result.getModule());
        verify(auditLogRepository).findById(auditId);
    }

    @Test
    void getAuditLogDetail_nonExistingId_throwsNotFound() {
        // Arrange
        String nonExistingId = "nonexistent";
        when(auditLogRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class, () -> auditService.getAuditLogDetail(nonExistingId));
        verify(auditMapper, never()).toAuditLogVO(any());
    }

    @Test
    void getComplianceReport_calculatesMetrics() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 6, 30, 23, 59);

        when(auditLogRepository.count(any(Specification.class))).thenReturn(100L, 5L);
        when(dataAccessLogRepository.count(any(Specification.class))).thenReturn(500L);
        when(systemEventRepository.count(any(Specification.class))).thenReturn(20L, 2L);

        // Act
        Map<String, Object> report = auditService.getComplianceReport(startTime, endTime);

        // Assert
        assertNotNull(report);
        assertEquals(100L, report.get("totalOperations"));
        assertEquals(5L, report.get("failedOperations"));
        assertEquals("95.0%", report.get("successRate"));
        assertEquals(500L, report.get("totalDataAccess"));
        assertEquals(20L, report.get("totalSystemEvents"));
        assertEquals(2L, report.get("criticalEvents"));
        assertEquals(startTime, report.get("startTime"));
        assertEquals(endTime, report.get("endTime"));
    }
}
