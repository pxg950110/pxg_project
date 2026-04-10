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
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "org_id", length = 36)
    private String orgId;

    @Column(name = "module", length = 50)
    private String module;

    @Column(name = "operation", length = 100)
    private String operation;

    @Column(name = "method", length = 200)
    private String method;

    @Column(name = "params", columnDefinition = "TEXT")
    private String params;

    @Column(name = "ip", length = 50)
    private String ip;

    @Column(name = "request_url", length = 500)
    private String requestUrl;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "response_msg", columnDefinition = "TEXT")
    private String responseMsg;

    @Column(name = "duration")
    private Long duration;

    @Column(name = "status")
    private Short status = 1;

    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
