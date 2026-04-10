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
public class EvaluationCreateDTO {

    private Long modelVersionId;

    @NotBlank(message = "评估名称不能为空")
    private String evalName;

    @NotBlank(message = "评估类型不能为空")
    private String evalType;

    private Long datasetId;

    private JsonNode metricsConfig;
}
