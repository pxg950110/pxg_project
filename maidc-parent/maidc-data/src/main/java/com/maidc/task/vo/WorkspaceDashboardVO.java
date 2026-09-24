package com.maidc.task.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class WorkspaceDashboardVO {

    private WelcomeInfo welcome;
    private MetricsInfo metrics;
    private List<MetricCard> cards;
    private List<TodoItem> todos;
    private TodoStats todoStats;
    private List<NotificationItem> notifications;
    private List<QuickAction> quickActions;
    /** 专病队列动态（FR6）：仅 CLINICAL/RESEARCH 组返回，其余组为 null */
    private List<CohortDigestItem> cohortDigest;

    @Data
    @Builder
    public static class WelcomeInfo {
        private String userName;
        private String date;
        private String role;
        /** CLINICAL / RESEARCH / DATA / GOVERNANCE */
        private String roleGroup;
        /** 机构名（按 orgId 查 m_institution，查不到为 null，前端不展示） */
        private String orgName;
        /** 科室名（用户 dept_id → m_institution，无 DEPT 范围/查不到为 null） */
        private String deptName;
    }

    @Data
    @Builder
    public static class MetricsInfo {
        private long modelCount;
        private long activeDeployments;
        private long dailyInferences;
        private long pendingApprovals;
    }

    /** 角色化指标卡，前端通用渲染 */
    @Data
    @Builder
    public static class MetricCard {
        private String key;
        private String label;
        private long value;
        private String suffix;
        private String icon;
        private String route;
        /** primary / danger / success / warning，可空 */
        private String tone;
    }

    /** 待办中心汇总统计 */
    @Data
    @Builder
    public static class TodoStats {
        private long today;
        private long overdue;
        private long total;
    }

    /** 工作台待办：通用待办 + 随访待办（随访扩展字段仅 taskType=FOLLOWUP 时有值） */
    @Data
    @Builder
    public static class TodoItem {
        private Long id;
        private String taskType;
        private String title;
        private String priority;
        private String status;
        private Long sourceId;
        private String sourceType;
        private String dueDate;
        private String createdAt;
        private Long patientId;
        private String patientName;
        private String stageName;
        private List<String> scales;
        private Integer overdueDays;
    }

    @Data
    @Builder
    public static class NotificationItem {
        private Long id;
        private String type;
        private String title;
        private String content;
        private boolean isRead;
        private String createdAt;
        private Long bizId;
        private String bizType;
    }

    @Data
    @Builder
    public static class QuickAction {
        private String key;
        private String label;
        private String icon;
        private String route;
        /** 前端 hasPermission 兜底过滤用，可空 */
        private String permission;
    }

    /** 专病队列动态条目（FR6）：SYNC_DONE / KB_ITEM_PUBLISHED / AI_SUGGEST_PENDING */
    @Data
    @Builder
    public static class CohortDigestItem {
        private String type;
        private String title;
        /** 展示时间 MM-dd HH:mm */
        private String time;
        /** 关联队列，无关联为空 */
        private Long cohortId;
    }
}
