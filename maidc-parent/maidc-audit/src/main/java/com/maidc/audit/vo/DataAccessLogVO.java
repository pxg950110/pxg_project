package com.maidc.audit.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DataAccessLogVO {

    private Long id;

    private Long userId;

    private String accessType;

    private String dataDomain;

    private String tableName;

    private Long recordId;

    private Long patientId;

    private String purpose;

    private Long dataVolume;

    private String ipAddress;

    private LocalDateTime createdAt;

    private Long orgId;
}
