package com.maidc.model.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.model.dto.ApprovalCreateDTO;
import com.maidc.model.dto.ApprovalReviewDTO;
import com.maidc.model.entity.ApprovalEntity;
import com.maidc.model.mapper.ModelMapper;
import com.maidc.model.repository.ApprovalRepository;
import com.maidc.model.vo.ApprovalVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private ApprovalRepository approvalRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ApprovalService approvalService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void submitApproval_withRiskAssessment_savesEntityWithJsonObject() {
        // Arrange
        ApprovalCreateDTO dto = new ApprovalCreateDTO();
        dto.setModelVersionId(10L);
        dto.setApprovalType("DEPLOY");
        dto.setRiskAssessment("low-risk");

        ApprovalEntity savedEntity = new ApprovalEntity();
        savedEntity.setId(1L);
        when(approvalRepository.save(any(ApprovalEntity.class))).thenReturn(savedEntity);
        when(modelMapper.toApprovalVO(any())).thenReturn(new ApprovalVO());

        // Act
        approvalService.submitApproval(dto);

        // Assert — capture the entity passed to save()
        ArgumentCaptor<ApprovalEntity> captor = ArgumentCaptor.forClass(ApprovalEntity.class);
        verify(approvalRepository).save(captor.capture());

        ApprovalEntity entity = captor.getValue();
        JsonNode riskNode = entity.getRiskAssessment();
        assertNotNull(riskNode, "riskAssessment should not be null when DTO provides a string");
        assertEquals("low-risk", riskNode.get("assessment").asText(),
                "riskAssessment JSON should contain {\"assessment\": \"low-risk\"}");
        assertEquals("PENDING", entity.getStatus());
        assertEquals(1, entity.getCurrentLevel());
    }

    @Test
    void submitApproval_withNullRiskAssessment_savesEntityWithNullRisk() {
        // Arrange
        ApprovalCreateDTO dto = new ApprovalCreateDTO();
        dto.setModelVersionId(10L);
        dto.setApprovalType("DEPLOY");
        dto.setRiskAssessment(null);

        ApprovalEntity savedEntity = new ApprovalEntity();
        savedEntity.setId(2L);
        when(approvalRepository.save(any(ApprovalEntity.class))).thenReturn(savedEntity);
        when(modelMapper.toApprovalVO(any())).thenReturn(new ApprovalVO());

        // Act
        approvalService.submitApproval(dto);

        // Assert
        ArgumentCaptor<ApprovalEntity> captor = ArgumentCaptor.forClass(ApprovalEntity.class);
        verify(approvalRepository).save(captor.capture());

        ApprovalEntity entity = captor.getValue();
        assertNull(entity.getRiskAssessment(), "riskAssessment should be null when DTO provides null");
    }

    @Test
    void reviewApproval_withApprovedResult_setsStatusToApproved() {
        // Arrange
        ApprovalEntity pending = new ApprovalEntity();
        pending.setId(1L);
        pending.setStatus("PENDING");
        when(approvalRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(approvalRepository.save(any(ApprovalEntity.class))).thenReturn(pending);
        when(modelMapper.toApprovalVO(any())).thenReturn(new ApprovalVO());

        ApprovalReviewDTO dto = new ApprovalReviewDTO();
        dto.setResult("APPROVED");
        dto.setResultComment("Looks good");

        // Act
        approvalService.reviewApproval(1L, dto, 42L);

        // Assert
        ArgumentCaptor<ApprovalEntity> captor = ArgumentCaptor.forClass(ApprovalEntity.class);
        verify(approvalRepository).save(captor.capture());

        ApprovalEntity entity = captor.getValue();
        assertEquals("APPROVED", entity.getStatus());
        assertEquals(42L, entity.getReviewedBy());
        assertEquals("Looks good", entity.getReviewComment());
        assertNotNull(entity.getReviewedAt());
    }

    @Test
    void reviewApproval_withRejectedResult_setsStatusToRejected() {
        // Arrange
        ApprovalEntity pending = new ApprovalEntity();
        pending.setId(2L);
        pending.setStatus("PENDING");
        when(approvalRepository.findById(2L)).thenReturn(Optional.of(pending));
        when(approvalRepository.save(any(ApprovalEntity.class))).thenReturn(pending);
        when(modelMapper.toApprovalVO(any())).thenReturn(new ApprovalVO());

        ApprovalReviewDTO dto = new ApprovalReviewDTO();
        dto.setResult("REJECTED");
        dto.setResultComment("Not ready");

        // Act
        approvalService.reviewApproval(2L, dto, 99L);

        // Assert
        ArgumentCaptor<ApprovalEntity> captor = ArgumentCaptor.forClass(ApprovalEntity.class);
        verify(approvalRepository).save(captor.capture());

        assertEquals("REJECTED", captor.getValue().getStatus());
    }

    @Test
    void reviewApproval_onNonPendingApproval_throwsException() {
        // Arrange — entity already approved
        ApprovalEntity approved = new ApprovalEntity();
        approved.setId(3L);
        approved.setStatus("APPROVED");
        when(approvalRepository.findById(3L)).thenReturn(Optional.of(approved));

        ApprovalReviewDTO dto = new ApprovalReviewDTO();
        dto.setResult("APPROVED");

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalService.reviewApproval(3L, dto, 42L));
        assertEquals(ErrorCode.APPROVAL_NOT_PENDING.getCode(), ex.getCode());
        verify(approvalRepository, never()).save(any());
    }
}
