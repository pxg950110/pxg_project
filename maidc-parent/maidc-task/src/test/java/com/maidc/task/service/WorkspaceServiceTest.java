package com.maidc.task.service;

import com.maidc.task.repository.WorkspaceMetricsRepository;
import com.maidc.task.vo.WorkspaceDashboardVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private PersonalTaskService personalTaskService;

    @Mock
    private WorkspaceMetricsRepository metricsRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void getDashboard_returnsAllSections() {
        when(metricsRepository.countModelByOrgId(anyLong())).thenReturn(28L);
        when(metricsRepository.countActiveDeploymentsByOrgId(anyLong())).thenReturn(8L);
        when(metricsRepository.countTodayInferencesByOrgId(anyLong())).thenReturn(12456L);
        when(metricsRepository.countPendingApprovalsByOrgId(anyLong())).thenReturn(5L);
        when(personalTaskService.getPendingTasks(anyLong())).thenReturn(List.of());

        WorkspaceDashboardVO result = workspaceService.getDashboard(1L, 1L);

        assertNotNull(result);
        assertNotNull(result.getWelcome());
        assertNotNull(result.getMetrics());
        assertNotNull(result.getTodos());
        assertNotNull(result.getQuickActions());
        assertEquals(28L, result.getMetrics().getModelCount());
        assertEquals(8L, result.getMetrics().getActiveDeployments());
        assertEquals(12456L, result.getMetrics().getDailyInferences());
        assertEquals(5L, result.getMetrics().getPendingApprovals());
        assertFalse(result.getQuickActions().isEmpty());
    }

    @Test
    void getDashboard_metricsReturnsZero_whenNoData() {
        when(metricsRepository.countModelByOrgId(anyLong())).thenReturn(0L);
        when(metricsRepository.countActiveDeploymentsByOrgId(anyLong())).thenReturn(0L);
        when(metricsRepository.countTodayInferencesByOrgId(anyLong())).thenReturn(0L);
        when(metricsRepository.countPendingApprovalsByOrgId(anyLong())).thenReturn(0L);
        when(personalTaskService.getPendingTasks(anyLong())).thenReturn(List.of());

        WorkspaceDashboardVO result = workspaceService.getDashboard(1L, 1L);

        assertEquals(0L, result.getMetrics().getModelCount());
        assertTrue(result.getTodos().isEmpty());
    }
}
