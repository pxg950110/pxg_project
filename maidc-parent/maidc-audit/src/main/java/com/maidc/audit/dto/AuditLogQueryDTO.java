package com.maidc.audit.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogQueryDTO {

    private String serviceName;

    private String operation;

    private String username;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private Integer page = 1;

    private Integer pageSize = 20;
}
