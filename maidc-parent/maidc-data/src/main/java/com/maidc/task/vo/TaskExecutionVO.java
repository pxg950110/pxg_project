package com.maidc.task.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskExecutionVO {

    private String id;
    private String taskId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration;
    private String resultMessage;
    private String errorMessage;
    private Long recordsProcessed;
    private String triggerType;
    private String createdBy;
}
