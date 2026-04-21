package com.maidc.task.service;

import com.maidc.task.repository.WorkspaceMetricsRepository;
import com.maidc.task.vo.WorkspaceDashboardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final PersonalTaskService personalTaskService;
    private final WorkspaceMetricsRepository metricsRepository;

    public WorkspaceDashboardVO getDashboard(Long userId, Long orgId) {
        return WorkspaceDashboardVO.builder()
                .welcome(buildWelcome(userId))
                .metrics(buildMetrics(orgId))
                .todos(personalTaskService.getPendingTasks(userId))
                .notifications(buildNotifications(userId))
                .quickActions(buildQuickActions())
                .build();
    }

    private WorkspaceDashboardVO.WelcomeInfo buildWelcome(Long userId) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE", Locale.CHINA));
        return WorkspaceDashboardVO.WelcomeInfo.builder()
                .userName("")
                .date(today)
                .role("")
                .build();
    }

    private WorkspaceDashboardVO.MetricsInfo buildMetrics(Long orgId) {
        return WorkspaceDashboardVO.MetricsInfo.builder()
                .modelCount(metricsRepository.countModelByOrgId(orgId))
                .activeDeployments(metricsRepository.countActiveDeploymentsByOrgId(orgId))
                .dailyInferences(metricsRepository.countTodayInferencesByOrgId(orgId))
                .pendingApprovals(metricsRepository.countPendingApprovalsByOrgId(orgId))
                .build();
    }

    private List<WorkspaceDashboardVO.QuickAction> buildQuickActions() {
        return List.of(
                WorkspaceDashboardVO.QuickAction.builder().key("new_model").label("新建模型").icon("plus-outlined").route("/model/list").build(),
                WorkspaceDashboardVO.QuickAction.builder().key("patient_query").label("患者查询").icon("search-outlined").route("/data/cdr/patients").build(),
                WorkspaceDashboardVO.QuickAction.builder().key("new_evaluation").label("新建评估").icon("experiment-outlined").route("/model/evaluations").build(),
                WorkspaceDashboardVO.QuickAction.builder().key("etl_task").label("ETL任务").icon("thunderbolt-outlined").route("/data/etl/pipelines").build()
        );
    }

    private List<WorkspaceDashboardVO.NotificationItem> buildNotifications(Long userId) {
        List<Object[]> rows = metricsRepository.findRecentMessagesByUserId(userId, 5);
        return rows.stream().map(row -> {
            Timestamp ts = (Timestamp) row[5];
            String createdAt = ts != null ? ts.toLocalDateTime().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
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
}
