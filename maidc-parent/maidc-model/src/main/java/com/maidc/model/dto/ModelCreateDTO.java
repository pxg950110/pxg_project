package com.maidc.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCreateDTO {

    @NotBlank(message = "模型编码不能为空")
    @Size(max = 32, message = "模型编码最长32个字符")
    private String modelCode;

    @NotBlank(message = "模型名称不能为空")
    @Size(max = 128, message = "模型名称最长128个字符")
    private String modelName;

    private String description;

    @NotBlank(message = "模型类型不能为空")
    private String modelType;

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    @NotBlank(message = "框架不能为空")
    private String framework;

    private JsonNode inputSchema;

    private JsonNode outputSchema;

    private String tags;

    private String license;

    private Long projectId;

    private Long orgId;
}
