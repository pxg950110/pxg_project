package com.maidc.audit.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DataAccessQueryDTO {

    private Long userId;

    private String dataDomain;

    private Long patientId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer page = 1;

    private Integer pageSize = 20;
}
