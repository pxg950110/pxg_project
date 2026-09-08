package com.maidc.audit.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogVO {

    private Long id;

    private String traceId;

    private Long userId;

    private String username;

    private String serviceName;

    private String operation;

    private String resourceType;

    private String resourceId;

    private String resourceName;

    private String requestMethod;

    private String requestUrl;

    private String requestParams;

    private Integer responseCode;

    private String responseMsg;

    private String ipAddress;

    private String userAgent;

    private Integer durationMs;

    private String status;

    private String errorMessage;

    private LocalDateTime createdAt;

    private Long orgId;
}
