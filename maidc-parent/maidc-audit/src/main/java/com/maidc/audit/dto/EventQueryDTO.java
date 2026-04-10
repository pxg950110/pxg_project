package com.maidc.audit.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventQueryDTO {

    private String eventType;

    private String severity;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer page = 1;

    private Integer pageSize = 20;
}
