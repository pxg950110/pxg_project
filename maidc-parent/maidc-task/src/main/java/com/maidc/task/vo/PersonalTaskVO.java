package com.maidc.task.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PersonalTaskVO {
    private Long id;
    private String title;
    private String description;
    private String taskType;
    private String priority;
    private String status;
    private Long assigneeId;
    private Long sourceId;
    private String sourceType;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
}
