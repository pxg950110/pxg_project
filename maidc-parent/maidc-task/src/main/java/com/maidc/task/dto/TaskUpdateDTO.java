package com.maidc.task.dto;

import lombok.Data;

@Data
public class TaskUpdateDTO {

    private String name;
    private String cronExpression;
    private String description;
    private String taskConfig;
}
