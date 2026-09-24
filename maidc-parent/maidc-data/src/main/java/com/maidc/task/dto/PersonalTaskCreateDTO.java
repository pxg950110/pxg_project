package com.maidc.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PersonalTaskCreateDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200)
    private String title;

    private String description;

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    private String priority;

    @NotNull(message = "指派人不能为空")
    private Long assigneeId;

    private Long sourceId;
    private String sourceType;
    private String dueDate;
}
