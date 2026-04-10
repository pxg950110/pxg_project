package com.maidc.data.dto;

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
public class EtlTaskCreateDTO {

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 128, message = "任务名称最长128个字符")
    private String name;

    @NotBlank(message = "数据源类型不能为空")
    private String sourceType;

    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    private String cronExpression;

    private JsonNode config;

    private Long orgId;
}
