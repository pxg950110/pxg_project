package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.DatasetEntity;
import com.maidc.data.entity.ProjectEntity;
import com.maidc.data.entity.ProjectMemberEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.DatasetRepository;
import com.maidc.data.repository.ProjectMemberRepository;
import com.maidc.data.repository.ProjectRepository;
import com.maidc.data.vo.ProjectVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private DatasetRepository datasetRepository;

    @Mock
    private DataMapper dataMapper;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void getProject_existingId_returnsProject() {
        // Arrange
        Long projectId = 1L;
        ProjectEntity entity = new ProjectEntity();
        entity.setId(projectId);
        entity.setName("肺癌AI辅助诊断研究");
        entity.setResearchType("CLINICAL");
        entity.setPiId(100L);
        entity.setStatus("DRAFT");
        entity.setStartDate(LocalDate.of(2025, 1, 1));
        entity.setEndDate(LocalDate.of(2026, 12, 31));

        ProjectVO expectedVO = ProjectVO.builder()
                .id(projectId)
                .name("肺癌AI辅助诊断研究")
                .researchType("CLINICAL")
                .piId(100L)
                .status("DRAFT")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .memberCount(0)
                .datasetCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findByIdAndIsDeletedFalse(projectId)).thenReturn(Optional.of(entity));
        when(dataMapper.toProjectVO(entity)).thenReturn(expectedVO);
        when(projectMemberRepository.findByProjectIdAndIsDeletedFalse(projectId)).thenReturn(Collections.emptyList());
        when(datasetRepository.findByProjectIdAndIsDeletedFalse(projectId)).thenReturn(Collections.emptyList());

        // Act
        ProjectVO result = projectService.getProject(projectId);

        // Assert
        assertNotNull(result);
        assertEquals(projectId, result.getId());
        assertEquals("肺癌AI辅助诊断研究", result.getName());
        assertEquals("DRAFT", result.getStatus());
        assertEquals(0, result.getMemberCount());
        assertEquals(0, result.getDatasetCount());
        verify(projectRepository).findByIdAndIsDeletedFalse(projectId);
        verify(dataMapper).toProjectVO(entity);
    }

    @Test
    void getProject_nonExistingId_throws() {
        // Arrange
        Long nonExistingId = 999L;
        when(projectRepository.findByIdAndIsDeletedFalse(nonExistingId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> projectService.getProject(nonExistingId));

        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
        verify(projectRepository).findByIdAndIsDeletedFalse(nonExistingId);
        verifyNoInteractions(dataMapper);
        verifyNoInteractions(projectMemberRepository);
        verifyNoInteractions(datasetRepository);
    }
}
