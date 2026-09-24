package com.maidc.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskCreateDTO {

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 200)
    private String name;

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    private String cronExpression;

    private String description;

    private String taskConfig;
}
