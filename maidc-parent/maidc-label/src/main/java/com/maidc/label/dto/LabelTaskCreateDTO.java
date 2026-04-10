package com.maidc.label.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class LabelTaskCreateDTO {

    @NotBlank(message = "任务名称不能为空")
    private String name;

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    @NotBlank(message = "数据集ID不能为空")
    private String datasetId;

    private String assigneeId;

    private List<String> labels;

    private String guidelines;
}
