package com.maidc.task.service;

import com.maidc.common.security.context.PermissionContext;
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
import com.maidc.task.vo.PersonalTaskVO;
import com.maidc.task.vo.WorkspaceDashboardVO;
import com.maidc.task.vo.WorkspaceDashboardVO.MetricCard;
import com.maidc.task.vo.WorkspaceDashboardVO.TodoItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    public static final String GROUP_CLINICAL = "CLINICAL";
    public static final String GROUP_RESEARCH = "RESEARCH";
    public static final String GROUP_DATA = "DATA";
    public static final String GROUP_GOVERNANCE = "GOVERNANCE";

    /** 角色码 → 角色组；遍历顺序即多角色用户归属优先级：临床 > 科研 > 数据 > 治理 */
    private static final Map<String, String> ROLE_TO_GROUP = new LinkedHashMap<>();
    private static final Map<String, String> ROLE_NAMES = Map.of(
            "doctor", "临床医生", "nurse", "护士",
            "researcher_pi", "科研负责人", "researcher", "科研成员",
            "data_admin", "数据管理员", "ai_engineer", "AI 工程师",
            "admin", "系统管理员", "auditor", "审计员");

    static {
        ROLE_TO_GROUP.put("doctor", GROUP_CLINICAL);
        ROLE_TO_GROUP.put("nurse", GROUP_CLINICAL);
        ROLE_TO_GROUP.put("researcher_pi", GROUP_RESEARCH);
        ROLE_TO_GROUP.put("researcher", GROUP_RESEARCH);
        ROLE_TO_GROUP.put("data_admin", GROUP_DATA);
        ROLE_TO_GROUP.put("ai_engineer", GROUP_DATA);
        ROLE_TO_GROUP.put("admin", GROUP_GOVERNANCE);
        ROLE_TO_GROUP.put("auditor", GROUP_GOVERNANCE);
    }

    private final PersonalTaskService personalTaskService;
    private final WorkspaceMetricsRepository metricsRepository;
    private final FollowupTaskService followupTaskService;
    private final DiseaseCohortRepository diseaseCohortRepository;
    private final DiseaseCohortPatientRepository diseaseCohortPatientRepository;
    private final DatasetRepository datasetRepository;
    private final PermissionStore permissionStore;
    private final InstitutionRepository institutionRepository;
    private final DiseaseCohortEventService cohortEventService;

    public WorkspaceDashboardVO getDashboard(Long userId, Long orgId, String username, List<String> roles) {
        String roleGroup = resolveRoleGroup(roles);
        String primaryRole = roles == null ? "" : roles.stream().filter(ROLE_NAMES::containsKey).findFirst().orElse("");
        WorkspaceDashboardVO.MetricsInfo metrics = buildMetrics(orgId);
        List<PersonalTaskVO> pending = personalTaskService.getPendingTasks(userId);
        List<TodoItem> todos = buildTodos(userId, roleGroup, pending);

        return WorkspaceDashboardVO.builder()
                .welcome(buildWelcome(userId, orgId, username, primaryRole, roleGroup))
                .metrics(metrics)
                .cards(buildCards(roleGroup, orgId, pending, metrics, userId))
                .todos(todos)
                .todoStats(buildTodoStats(todos))
                .notifications(buildNotifications(userId))
                .quickActions(buildQuickActions(roleGroup))
                .cohortDigest(buildCohortDigest(roleGroup, orgId))
                .build();
    }

    String resolveRoleGroup(List<String> roles) {
        if (roles != null) {
            for (Map.Entry<String, String> entry : ROLE_TO_GROUP.entrySet()) {
                if (roles.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        // 角色缺失（旧 token / 服务直连）回退 v1 行为：模型指标
        return GROUP_DATA;
    }

    private WorkspaceDashboardVO.WelcomeInfo buildWelcome(Long userId, Long orgId, String username, String primaryRole, String roleGroup) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE", Locale.CHINA));
        Map<String, String> orgDept = resolveOrgDeptNames(userId, orgId);
        return WorkspaceDashboardVO.WelcomeInfo.builder()
                .userName(username == null ? "" : username)
                .date(today)
                .role(ROLE_NAMES.getOrDefault(primaryRole, ""))
                .roleGroup(roleGroup)
                .orgName(orgDept.get("orgName"))
                .deptName(orgDept.get("deptName"))
                .build();
    }

    /** 机构/科室名解析（FR4）：任一步不可得即置 null，欢迎区永不因此失败 */
    private Map<String, String> resolveOrgDeptNames(Long userId, Long orgId) {
        String orgName = safeInstitutionName(orgId);
        String deptName = null;
        try {
            PermissionContext ctx = permissionStore.load(userId);
            if (ctx != null && ctx.getDeptId() != null) {
                deptName = safeInstitutionName(ctx.getDeptId());
            }
        } catch (Exception e) {
            log.warn("Workspace dept name unavailable: {}", e.getMessage());
        }
        Map<String, String> result = new LinkedHashMap<>();
        result.put("orgName", orgName);
        result.put("deptName", deptName);
        return result;
    }

    private String safeInstitutionName(Long id) {
        if (id == null) return null;
        try {
            return institutionRepository.findById(id).map(InstitutionEntity::getName).orElse(null);
        } catch (Exception e) {
            log.warn("Workspace institution name unavailable for id {}: {}", id, e.getMessage());
            return null;
        }
    }

    private WorkspaceDashboardVO.MetricsInfo buildMetrics(Long orgId) {
        return WorkspaceDashboardVO.MetricsInfo.builder()
                .modelCount(metricsRepository.countModelByOrgId(orgId))
                .activeDeployments(metricsRepository.countActiveDeploymentsByOrgId(orgId))
                .dailyInferences(metricsRepository.countTodayInferencesByOrgId(orgId))
                .pendingApprovals(metricsRepository.countPendingApprovalsByOrgId(orgId))
                .build();
    }

    // ==================== 角色化指标卡 ====================

    private List<MetricCard> buildCards(String roleGroup, Long orgId, List<PersonalTaskVO> pending,
                                        WorkspaceDashboardVO.MetricsInfo metrics, Long userId) {
        return switch (roleGroup) {
            case GROUP_CLINICAL -> clinicalCards(userId);
            case GROUP_RESEARCH -> researchCards(orgId, pending);
            case GROUP_GOVERNANCE -> governanceCards(orgId);
            default -> dataCards(metrics, orgId); // DATA
        };
    }

    private List<MetricCard> clinicalCards(Long userId) {
        Map<String, Object> digest = followupDigestSafe(userId);
        return List.of(
                card("followup_today", "今日随访到期", num(digest.get("todayCount")), "项", "schedule", "/data/cdr/disease", "primary"),
                card("followup_overdue", "已超期随访", num(digest.get("overdueCount")), "项", "alert", "/data/cdr/disease", "danger"),
                card("followup_patients", "在管随访患者", num(digest.get("activePatients")), "人", "team", "/data/cdr/disease", null),
                card("followup_week_done", "本周完成随访", num(digest.get("weekDoneCount")), "例", "check-circle", "/data/cdr/disease", "success"));
    }

    private List<MetricCard> researchCards(Long orgId, List<PersonalTaskVO> pending) {
        long pendingApprovals = pending.stream().filter(t -> "APPROVAL".equals(t.getTaskType())).count();
        return List.of(
                card("cohort_active", "在管专病队列", safe(() -> diseaseCohortRepository.countByOrgId(orgId)), "个", "database", "/data/cdr/disease", null),
                card("cohort_patients", "队列患者总数", safe(diseaseCohortPatientRepository::count), "人", "team", "/data/cdr/disease", null),
                card("pending_approvals", "待审批事项", pendingApprovals, "项", "audit", "/model/approvals", "warning"),
                card("datasets", "研究数据集", safe(datasetRepository::count), "个", "appstore", "/data/rdr/datasets", null));
    }

    /** DATA 组：PRD FR1 = 模型总数/活跃部署/今日推理/待处理质控（待审批数保留在 metrics 字段） */
    private List<MetricCard> dataCards(WorkspaceDashboardVO.MetricsInfo metrics, Long orgId) {
        return List.of(
                card("model_count", "模型总数", metrics.getModelCount(), "个", "experiment", "/model/list", null),
                card("active_deployments", "活跃部署", metrics.getActiveDeployments(), "个", "rocket", "/model/deployments", null),
                card("daily_inferences", "今日推理", metrics.getDailyInferences(), "次", "thunderbolt", "/model/inference-logs", null),
                card("quality_pending", "待处理质控", safe(() -> metricsRepository.countPendingQuarantineByOrgId(orgId)), "条", "shield", "/data/cdr/quality-results", "warning"));
    }

    /** GOVERNANCE 组：PRD FR1 = 平台用户/今日审计事件/今日权限拒绝/活跃告警 */
    private List<MetricCard> governanceCards(Long orgId) {
        return List.of(
                card("user_count", "平台用户", safe(() -> metricsRepository.countUsersByOrgId(orgId)), "人", "user", "/system/users", null),
                card("audit_today", "今日审计事件", safe(() -> metricsRepository.countTodayAuditEventsByOrgId(orgId)), "条", "file-search", "/audit/operations", null),
                card("perm_denied_today", "今日权限拒绝", safe(() -> metricsRepository.countTodayPermissionDeniedByOrgId(orgId)), "次", "alert", "/audit/system-events", "danger"),
                card("active_alerts", "活跃告警", safe(() -> metricsRepository.countActiveAlertsByOrgId(orgId)), "条", "alert", "/alert/active", "warning"));
    }

    private MetricCard card(String key, String label, long value, String suffix, String icon, String route, String tone) {
        return MetricCard.builder().key(key).label(label).value(value).suffix(suffix)
                .icon(icon).route(route).tone(tone).build();
    }

    private Map<String, Object> followupDigestSafe(Long userId) {
        try {
            Map<String, Object> digest = followupTaskService.workspaceDigest(userId, LocalDate.now());
            return digest == null ? Map.of() : digest;
        } catch (Exception e) {
            log.warn("Followup digest unavailable, degrade to zero cards: {}", e.getMessage());
            return Map.of();
        }
    }

    private long num(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private long safe(Supplier<Long> counter) {
        try {
            return counter.get();
        } catch (Exception e) {
            log.warn("Metric count failed, degrade to 0: {}", e.getMessage());
            return 0L;
        }
    }

    // ==================== 待办中心 ====================

    private List<TodoItem> buildTodos(Long userId, String roleGroup, List<PersonalTaskVO> pending) {
        List<TodoItem> todos = new ArrayList<>(personalTodos(pending));
        if (GROUP_CLINICAL.equals(roleGroup)) {
            todos.addAll(followupTodos(userId));
        }
        todos.sort(Comparator.comparing(t -> t.getDueDate() == null ? "9999-12-31" : t.getDueDate()));
        return todos.size() > 20 ? new ArrayList<>(todos.subList(0, 20)) : todos;
    }

    private List<TodoItem> personalTodos(List<PersonalTaskVO> pending) {
        LocalDate today = LocalDate.now();
        return pending.stream().map(t -> TodoItem.builder()
                .id(t.getId())
                .taskType(t.getTaskType())
                .title(t.getTitle())
                .priority(t.getPriority())
                .status(t.getStatus())
                .sourceId(t.getSourceId())
                .sourceType(t.getSourceType())
                .dueDate(t.getDueDate() == null ? null : t.getDueDate().toLocalDate().toString())
                .createdAt(t.getCreatedAt() == null ? null
                        : t.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .overdueDays(t.getDueDate() == null ? 0
                        : (int) Math.max(0, today.toEpochDay() - t.getDueDate().toLocalDate().toEpochDay()))
                .build()).toList();
    }

    @SuppressWarnings("unchecked")
    private List<TodoItem> followupTodos(Long userId) {
        try {
            Map<String, Object> digest = followupTaskService.workspaceDigest(userId, LocalDate.now());
            List<Map<String, Object>> tasks = (List<Map<String, Object>>) digest.getOrDefault("tasks", List.of());
            return tasks.stream().map(t -> TodoItem.builder()
                    .id(((Number) t.get("id")).longValue())
                    .taskType("FOLLOWUP")
                    .title(t.get("patientName") + " · " + t.get("stageName") + "随访")
                    .priority(((Number) t.get("overdueDays")).intValue() > 0 ? "HIGH" : "MEDIUM")
                    .status("PENDING")
                    .sourceId(((Number) t.get("id")).longValue())
                    .sourceType("FOLLOWUP")
                    .dueDate((String) t.get("dueDate"))
                    .patientId(t.get("patientId") == null ? null : ((Number) t.get("patientId")).longValue())
                    .patientName((String) t.get("patientName"))
                    .stageName((String) t.get("stageName"))
                    .scales((List<String>) t.get("requiredScales"))
                    .overdueDays(((Number) t.get("overdueDays")).intValue())
                    .build()).toList();
        } catch (Exception e) {
            log.warn("Followup todos unavailable, degrade to personal tasks only: {}", e.getMessage());
            return List.of();
        }
    }

    private WorkspaceDashboardVO.TodoStats buildTodoStats(List<TodoItem> todos) {
        String today = LocalDate.now().toString();
        return WorkspaceDashboardVO.TodoStats.builder()
                .today(todos.stream().filter(t -> today.equals(t.getDueDate())).count())
                .overdue(todos.stream().filter(t -> t.getOverdueDays() != null && t.getOverdueDays() > 0).count())
                .total(todos.size())
                .build();
    }

    // ==================== 快捷入口（服务端按角色组，前端 hasPermission 兜底） ====================

    private List<WorkspaceDashboardVO.QuickAction> buildQuickActions(String roleGroup) {
        return switch (roleGroup) {
            case GROUP_CLINICAL -> List.of(
                    action("patient_search", "患者检索", "search", "/data/cdr/patients", "cdr:read"),
                    action("clinical_search", "临床检索", "profile", "/data/cdr/search", "cdr:read"),
                    action("disease_kb", "专病知识库", "book", "/data/cdr/disease-kb", "cdr:read"),
                    action("followup_workbench", "随访工作台", "medicine-box", "/followup/workbench", "disease:followup:work"));
            case GROUP_RESEARCH -> List.of(
                    action("cohort_manage", "专病队列", "database", "/data/cdr/disease", "cdr:read"),
                    action("clinical_search", "临床检索", "profile", "/data/cdr/search", "cdr:read"),
                    action("disease_kb", "知识库问答", "book", "/data/cdr/disease-kb", "cdr:read"),
                    action("rdr_projects", "研究项目", "project", "/data/rdr/projects", "rdr:read"),
                    action("rdr_datasets", "数据集", "appstore", "/data/rdr/datasets", "rdr:read"));
            case GROUP_GOVERNANCE -> List.of(
                    action("overview", "系统总览", "dashboard", "/dashboard/overview", null),
                    action("audit_ops", "操作审计", "file-search", "/audit/operations", null),
                    action("audit_data", "数据访问审计", "search", "/audit/data-access", null),
                    action("users", "用户管理", "user", "/system/users", null),
                    action("alerts", "告警中心", "alert", "/alert/active", null));
            default -> List.of(
                    action("etl_pipelines", "ETL 管道", "sync", "/etl/pipelines", null),
                    action("datasources", "数据源", "database", "/etl/datasources", null),
                    action("quality", "质量检测", "shield", "/data/cdr/quality-results", null),
                    action("new_model", "新建模型", "plus", "/model/list", null),
                    action("new_eval", "新建评估", "experiment", "/model/evaluations", null));
        };
    }

    private WorkspaceDashboardVO.QuickAction action(String key, String label, String icon, String route, String permission) {
        return WorkspaceDashboardVO.QuickAction.builder()
                .key(key).label(label).icon(icon).route(route).permission(permission).build();
    }

    private List<WorkspaceDashboardVO.NotificationItem> buildNotifications(Long userId) {
        List<Object[]> rows = metricsRepository.findRecentMessagesByUserId(userId, 5);
        return rows.stream().map(row -> {
            Timestamp ts = (Timestamp) row[5];
            String createdAt = ts != null ? ts.toLocalDateTime().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
            return WorkspaceDashboardVO.NotificationItem.builder()
                    .id(((Number) row[0]).longValue())
                    .type((String) row[1])
                    .title((String) row[2])
                    .content((String) row[3])
                    .isRead(Boolean.TRUE.equals(row[4]))
                    .createdAt(createdAt)
                    .build();
        }).toList();
    }

    // ==================== 专病队列动态（FR6，仅 CLINICAL/RESEARCH 组） ====================

    private List<WorkspaceDashboardVO.CohortDigestItem> buildCohortDigest(String roleGroup, Long orgId) {
        if (!GROUP_CLINICAL.equals(roleGroup) && !GROUP_RESEARCH.equals(roleGroup)) {
            return null;
        }
        try {
            return cohortEventService.latest(orgId).stream()
                    .map(this::toDigestItem)
                    .toList();
        } catch (Exception e) {
            log.warn("Cohort digest build failed, degrade to empty: {}", e.getMessage());
            return List.of();
        }
    }

    private WorkspaceDashboardVO.CohortDigestItem toDigestItem(DiseaseCohortEventEntity event) {
        String time = event.getCreatedAt() == null ? ""
                : event.getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        return WorkspaceDashboardVO.CohortDigestItem.builder()
                .type(event.getEventType())
                .title(event.getEventTitle())
                .time(time)
                .cohortId(event.getCohortId())
                .build();
    }
}
