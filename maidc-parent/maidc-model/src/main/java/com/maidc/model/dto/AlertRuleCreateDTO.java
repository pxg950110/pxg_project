package com.maidc.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleCreateDTO {

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    private Long targetId;

    private JsonNode conditionExpr;

    @NotBlank(message = "严重级别不能为空")
    private String severity;

    private JsonNode notifyChannels;
}
