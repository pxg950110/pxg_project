package com.maidc.task.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskVO {

    private String id;
    private String taskName;
    private String taskType;
    private String cronExpression;
    private String description;
    private String status;
    private String taskConfig;
    private LocalDateTime lastExecutionTime;
    private LocalDateTime nextExecutionTime;
    private Integer failureCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
