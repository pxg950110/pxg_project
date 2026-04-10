package com.maidc.audit.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DataAccessLogVO {

    private String id;

    private String userId;

    private String username;

    private String orgId;

    private String dataType;

    private String dataId;

    private String action;

    private String patientId;

    private String accessPurpose;

    private String ip;

    private LocalDateTime createdAt;
}
