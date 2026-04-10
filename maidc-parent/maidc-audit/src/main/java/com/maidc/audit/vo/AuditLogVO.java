package com.maidc.audit.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogVO {

    private String id;

    private String traceId;

    private String userId;

    private String username;

    private String orgId;

    private String module;

    private String operation;

    private String method;

    private String params;

    private String ip;

    private String requestUrl;

    private String requestMethod;

    private Integer responseCode;

    private String responseMsg;

    private Long duration;

    private Short status;

    private String errorMsg;

    private LocalDateTime createdAt;
}
