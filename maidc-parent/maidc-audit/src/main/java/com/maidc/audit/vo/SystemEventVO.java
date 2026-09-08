package com.maidc.audit.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemEventVO {

    private Long id;

    private String eventType;

    private String eventLevel;

    private String source;

    private String eventTitle;

    private String eventDetail;

    private String eventData;

    private Boolean resolved;

    private String resolvedBy;

    private LocalDateTime resolvedAt;

    private LocalDateTime createdAt;

    private Long orgId;
}
