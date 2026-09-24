package com.maidc.common.log.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperationLogData {

    private String serviceName;
    private String operation;
    private String resourceType;
    private String resourceId;
    private String resourceName;
    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private int durationMs;
    private String status;
    private String errorMessage;
    private String ipAddress;
    private String userAgent;
    private String traceId;
    private Long userId;
    private String username;
    private Long orgId;
}
