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
public class InferenceRequestDTO {

    @NotBlank(message = "请求ID不能为空")
    private String requestId;

    private Long patientId;

    private Long encounterId;

    private JsonNode input;
}
