package com.maidc.task.service;

import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.scope.DataScope;
import com.maidc.common.security.store.PermissionStore;
import com.maidc.data.entity.DiseaseCohortEventEntity;
import com.maidc.data.entity.InstitutionEntity;
import com.maidc.data.repository.DatasetRepository;
import com.maidc.data.repository.DiseaseCohortPatientRepository;
import com.maidc.data.repository.DiseaseCohortRepository;
import com.maidc.data.repository.InstitutionRepository;
import com.maidc.data.service.DiseaseCohortEventService;
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
import java.util.Optional;

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

    @Mock
    private PermissionStore permissionStore;

    @Mock
    private InstitutionRepository institutionRepository;

    @Mock
    private DiseaseCohortEventService cohortEventService;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void getDashboard_dataGroup_returnsModelCardsAndEtlActions() {
        when(metricsRepository.countModelByOrgId(anyLong())).thenReturn(28L);
        when(metricsRepository.countActiveDeploymentsByOrgId(anyLong())).thenReturn(8L);
        when(metricsRepository.countTodayInferencesByOrgId(anyLong())).thenReturn(12456L);
        when(metricsRepository.countPendingApprovalsByOrgId(anyLong())).thenReturn(5L);
        when(metricsRepository.countPendingQuarantineByOrgId(anyLong())).thenReturn(17L);
        when(personalTaskService.getPendingTasks(anyLong())).thenReturn(List.of());

        WorkspaceDashboardVO result = workspaceService.getDashboard(1L, 1L, "data_admin", List.of("data_admin"));

        assertEquals(WorkspaceService.GROUP_DATA, result.getWelcome().getRoleGroup());
        assertEquals("数据管理员", result.getWelcome().getRole());
        assertEquals(28L, result.getMetrics().getModelCount());
        assertEquals(4, result.getCards().size());
        assertEquals("model_count", result.getCards().get(0).getKey());
        assertEquals(28L, result.getCards().get(0).getValue());
        // PRD FR1：DATA 组第 4 卡 = 待处理质控（待审批数保留在 metrics 字段）
        assertEquals("quality_pending", result.getCards().get(3).getKey());
        assertEquals(17L, result.getCards().get(3).getValue());
        assertEquals("warning", result.getCards().get(3).getTone());
        assertEquals(5L, result.getMetrics().getPendingApprovals());
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

    @Test
    void getDashboard_governanceGroup_returnsExclusiveCards() {
        when(metricsRepository.countUsersByOrgId(1L)).thenReturn(36L);
        when(metricsRepository.countTodayAuditEventsByOrgId(1L)).thenReturn(1240L);
        when(metricsRepository.countTodayPermissionDeniedByOrgId(1L)).thenReturn(3L);
        when(metricsRepository.countActiveAlertsByOrgId(1L)).thenReturn(2L);
        when(personalTaskService.getPendingTasks(anyLong())).thenReturn(List.of());

        WorkspaceDashboardVO result = workspaceService.getDashboard(1L, 1L, "auditor1", List.of("auditor"));

        assertEquals(WorkspaceService.GROUP_GOVERNANCE, result.getWelcome().getRoleGroup());
        assertEquals("审计员", result.getWelcome().getRole());
        assertEquals(4, result.getCards().size());
        assertEquals("user_count", result.getCards().get(0).getKey());
        assertEquals(36L, result.getCards().get(0).getValue());
        assertEquals("audit_today", result.getCards().get(1).getKey());
        assertEquals("perm_denied_today", result.getCards().get(2).getKey());
        assertEquals("danger", result.getCards().get(2).getTone());
        assertEquals("active_alerts", result.getCards().get(3).getKey());
        assertEquals("warning", result.getCards().get(3).getTone());
        assertTrue(result.getQuickActions().stream().anyMatch(a -> "audit_ops".equals(a.getKey())));
    }

    @Test
    void getDashboard_welcome_resolvesDeptAndOrgNames() {
        PermissionContext ctx = PermissionContext.builder().userId(1L).deptId(5L).dataScope(DataScope.DEPT).build();
        when(permissionStore.load(1L)).thenReturn(ctx);
        InstitutionEntity dept = new InstitutionEntity();
        dept.setName("耳鼻喉科");
        InstitutionEntity org = new InstitutionEntity();
        org.setName("市一医院");
        when(institutionRepository.findById(5L)).thenReturn(Optional.of(dept));
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(org));
        when(personalTaskService.getPendingTasks(anyLong())).thenReturn(List.of());

        WorkspaceDashboardVO result = workspaceService.getDashboard(1L, 1L, "doctor1", List.of("doctor"));

        assertEquals("耳鼻喉科", result.getWelcome().getDeptName());
        assertEquals("市一医院", result.getWelcome().getOrgName());
    }

    @Test
    void getDashboard_welcome_withoutDeptContext_leavesNamesNull() {
        when(permissionStore.load(1L)).thenReturn(null);
        when(personalTaskService.getPendingTasks(anyLong())).thenReturn(List.of());

        WorkspaceDashboardVO result = workspaceService.getDashboard(1L, 1L, "admin1", List.of("admin"));

        assertNull(result.getWelcome().getDeptName());
        assertNull(result.getWelcome().getOrgName());
    }

    @Test
    void getDashboard_researchGroup_returnsCohortDigest() {
        when(personalTaskService.getPendingTasks(anyLong())).thenReturn(List.of());
        when(diseaseCohortRepository.countByOrgId(1L)).thenReturn(4L);
        when(datasetRepository.count()).thenReturn(2L);
        DiseaseCohortEventEntity sync = new DiseaseCohortEventEntity();
        sync.setEventType("SYNC_DONE");
        sync.setEventTitle("队列「CRS」同步完成：新增 3 人，在管 128 人");
        sync.setCohortId(7L);
        sync.setOrgId(1L);
        DiseaseCohortEventEntity kb = new DiseaseCohortEventEntity();
        kb.setEventType("KB_ITEM_PUBLISHED");
        kb.setEventTitle("知识库《EPOS2020 解读》已发布");
        kb.setCohortId(7L);
        kb.setOrgId(1L);
        when(cohortEventService.latest(1L)).thenReturn(List.of(sync, kb));

        WorkspaceDashboardVO result = workspaceService.getDashboard(1L, 1L, "li_pi", List.of("researcher_pi"));

        assertNotNull(result.getCohortDigest());
        assertEquals(2, result.getCohortDigest().size());
        assertEquals("SYNC_DONE", result.getCohortDigest().get(0).getType());
        assertEquals(7L, result.getCohortDigest().get(0).getCohortId());
        assertEquals("知识库《EPOS2020 解读》已发布", result.getCohortDigest().get(1).getTitle());
    }

    @Test
    void getDashboard_dataGroup_cohortDigestIsNull_andClinicalDigestDegradesOnFailure() {
        // DATA 组不返回队列动态
        when(metricsRepository.countModelByOrgId(anyLong())).thenReturn(1L);
        when(metricsRepository.countActiveDeploymentsByOrgId(anyLong())).thenReturn(0L);
        when(metricsRepository.countTodayInferencesByOrgId(anyLong())).thenReturn(0L);
        when(metricsRepository.countPendingApprovalsByOrgId(anyLong())).thenReturn(0L);
        when(metricsRepository.countPendingQuarantineByOrgId(anyLong())).thenReturn(0L);
        when(personalTaskService.getPendingTasks(anyLong())).thenReturn(List.of());
        WorkspaceDashboardVO dataResult = workspaceService.getDashboard(1L, 1L, "data_admin", List.of("data_admin"));
        assertNull(dataResult.getCohortDigest());

        // CLINICAL 组动态源查询失败 → 降级为空列表（接口仍可用）
        when(followupTaskService.workspaceDigest(anyLong(), any(LocalDate.class))).thenReturn(Map.of());
        when(cohortEventService.latest(anyLong())).thenThrow(new RuntimeException("c_cohort_event unavailable"));
        WorkspaceDashboardVO clinicalResult = workspaceService.getDashboard(1L, 1L, "nurse1", List.of("nurse"));
        assertNotNull(clinicalResult.getCohortDigest());
        assertTrue(clinicalResult.getCohortDigest().isEmpty());
    }
}
