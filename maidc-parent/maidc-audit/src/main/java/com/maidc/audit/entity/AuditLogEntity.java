package com.maidc.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "a_audit_log", schema = "audit")
public class AuditLogEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "service_name", length = 64)
    private String serviceName;

    @Column(name = "operation", length = 32)
    private String operation;

    @Column(name = "resource_type", length = 32)
    private String resourceType;

    @Column(name = "resource_id", length = 64)
    private String resourceId;

    @Column(name = "resource_name", length = 128)
    private String resourceName;

    @Column(name = "request_method", length = 8)
    private String requestMethod;

    @Column(name = "request_url", length = 256)
    private String requestUrl;

    @Column(name = "request_params", columnDefinition = "JSONB")
    private String requestParams;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "response_msg", length = 256)
    private String responseMsg;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 256)
    private String userAgent;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "status", length = 16)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "org_id")
    private Long orgId;
}
