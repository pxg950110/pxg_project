package com.maidc.task.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class WorkspaceDashboardVO {

    private WelcomeInfo welcome;
    private MetricsInfo metrics;
    private List<PersonalTaskVO> todos;
    private List<NotificationItem> notifications;
    private List<QuickAction> quickActions;

    @Data
    @Builder
    public static class WelcomeInfo {
        private String userName;
        private String date;
        private String role;
    }

    @Data
    @Builder
    public static class MetricsInfo {
        private long modelCount;
        private long activeDeployments;
        private long dailyInferences;
        private long pendingApprovals;
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
    }
}
