package com.maidc.audit.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemEventVO {

    private String id;

    private String eventType;

    private String eventSource;

    private String severity;

    private String title;

    private String detail;

    private String operator;

    private String ip;

    private LocalDateTime createdAt;
}
