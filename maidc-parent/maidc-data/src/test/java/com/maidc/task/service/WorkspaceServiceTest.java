package com.maidc.task.service;

import com.maidc.data.repository.DatasetRepository;
import com.maidc.data.repository.DiseaseCohortPatientRepository;
import com.maidc.data.repository.DiseaseCohortRepository;
import com.maidc.data.service.followup.FollowupTaskService;
import com.maidc.task.repository.WorkspaceMetricsRepository;
import com.maidc.task.vo.WorkspaceDashboardVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private PersonalTaskService personalTaskService;

    @Mock
    private WorkspaceMetricsRepository metricsRepository;

    @Mock
    private FollowupTaskService followupTaskService;

    @Mock
    private DiseaseCohortRepository diseaseCohortRepository;

    @Mock
    private DiseaseCohortPatientRepository diseaseCohortPatientRepository;

    @Mock
    private DatasetRepository datasetRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void getDashboard_dataGroup_returnsModelCardsAndEtlActions() {
        when(metricsRepository.countModelByOrgId(anyLong())).thenReturn(28L);
        when(metricsRepository.countActiveDeploymentsByOrgId(anyLong())).thenReturn(8L);
        when(metricsRepository.countTodayInferencesByOrgId(anyLong())).thenReturn(12456L);
        when(metricsRepository.countPendingApprovalsByOrgId(anyLong())).thenReturn(5L);
        when(personalTaskService.getPendingTasks(anyLong())).thenReturn(List.of());

        WorkspaceDashboardVO result = workspaceService.getDashboard(1L, 1L, "data_admin", List.of("data_admin"));

        assertEquals(WorkspaceService.GROUP_DATA, result.getWelcome().getRoleGroup());
        assertEquals("数据管理员", result.getWelcome().getRole());
        assertEquals(28L, result.getMetrics().getModelCount());
        assertEquals(4, result.getCards().size());
        assertEquals("model_count", result.getCards().get(0).getKey());
        assertEquals(28L, result.getCards().get(0).getValue());
        assertTrue(result.getQuickActions().stream().anyMatch(a -> "etl_pipelines".equals(a.getKey())));
        assertEquals(0L, result.getTodoStats().getTotal());
    }

    @Test
    void getDashboard_unknownRoles_fallbackToDataGroup() {
        when(personalTaskService.getPendingTasks(anyLong())).thenReturn(List.of());

        WorkspaceDashboardVO result = workspaceService.getDashboard(1L, 1L, "", List.of());

        assertEquals(WorkspaceService.GROUP_DATA, result.getWelcome().getRoleGroup());
        assertNotNull(result.getCards());
        assertFalse(result.getQuickActions().isEmpty());
    }

    @Test
    void getDashboard_clinicalGroup_includesFollowupCardsAndTodos() {
        Map<String, Object> digest = new HashMap<>();
        digest.put("todayCount", 3L);
        digest.put("overdueCount", 2L);
        digest.put("weekDoneCount", 9L);
        digest.put("activePatients", 86L);
        Map<String, Object> task = new HashMap<>();
        task.put("id", 88L);
        task.put("stageName", "术后3月");
        task.put("dueDate", LocalDate.now().toString());
        task.put("overdueDays", 0L);
        task.put("patientId", 1001L);
        task.put("patientName", "王建国");
        task.put("requiredScales", List.of("SNOT-22", "VAS"));
        Map<String, Object> overdueTask = new HashMap<>(task);
        overdueTask.put("id", 89L);
        overdueTask.put("dueDate", LocalDate.now().minusDays(2).toString());
        overdueTask.put("overdueDays", 2L);
        digest.put("tasks", List.of(task, overdueTask));
        when(followupTaskService.workspaceDigest(eq(1L), any(LocalDate.class))).thenReturn(digest);
        when(personalTaskService.getPendingTasks(anyLong())).thenReturn(List.of());

        WorkspaceDashboardVO result = workspaceService.getDashboard(1L, 1L, "doctor1", List.of("doctor"));

        assertEquals(WorkspaceService.GROUP_CLINICAL, result.getWelcome().getRoleGroup());
        assertEquals("followup_today", result.getCards().get(0).getKey());
        assertEquals(3L, result.getCards().get(0).getValue());
        assertEquals("danger", result.getCards().get(1).getTone());
        assertEquals(2, result.getTodos().size());
        assertEquals("FOLLOWUP", result.getTodos().get(0).getTaskType());
        // 超期（dueDate 更早）排前且高优先级
        assertEquals(2, result.getTodos().get(0).getOverdueDays());
        assertEquals("HIGH", result.getTodos().get(0).getPriority());
        assertEquals("王建国", result.getTodos().get(1).getPatientName());
        // todoStats 是列表口径：2 条待办中 1 条超期、1 条今日到期（digest.overdueCount=2 是卡片口径）
        assertEquals(1L, result.getTodoStats().getOverdue());
        assertEquals(1L, result.getTodoStats().getToday());
        assertEquals(2L, result.getTodoStats().getTotal());
        assertTrue(result.getQuickActions().stream().anyMatch(a -> "followup_workbench".equals(a.getKey())));
    }

    @Test
    void getDashboard_researchGroup_countsCohortsAndDatasets() {
        when(personalTaskService.getPendingTasks(anyLong())).thenReturn(List.of());
        when(diseaseCohortRepository.countByOrgId(1L)).thenReturn(4L);
        when(diseaseCohortPatientRepository.count()).thenReturn(1286L);
        when(datasetRepository.count()).thenReturn(12L);

        WorkspaceDashboardVO result = workspaceService.getDashboard(1L, 1L, "li_pi", List.of("researcher_pi", "researcher"));

        assertEquals(WorkspaceService.GROUP_RESEARCH, result.getWelcome().getRoleGroup());
        assertEquals("科研负责人", result.getWelcome().getRole());
        assertEquals("cohort_active", result.getCards().get(0).getKey());
        assertEquals(4L, result.getCards().get(0).getValue());
        assertEquals(1286L, result.getCards().get(1).getValue());
        assertEquals(12L, result.getCards().get(3).getValue());
        assertTrue(result.getQuickActions().stream().anyMatch(a -> "rdr_projects".equals(a.getKey())));
    }

    @Test
    void getDashboard_followupSourceFailure_degradesToPersonalTodosOnly() {
        when(followupTaskService.workspaceDigest(anyLong(), any(LocalDate.class)))
                .thenThrow(new RuntimeException("c_followup_task unavailable"));
        when(personalTaskService.getPendingTasks(anyLong())).thenReturn(List.of());

        WorkspaceDashboardVO result = workspaceService.getDashboard(1L, 1L, "nurse1", List.of("nurse"));

        assertEquals(WorkspaceService.GROUP_CLINICAL, result.getWelcome().getRoleGroup());
        assertTrue(result.getTodos().isEmpty());
        assertEquals(0L, result.getCards().get(0).getValue());
        assertNotNull(result.getTodoStats());
    }
}
