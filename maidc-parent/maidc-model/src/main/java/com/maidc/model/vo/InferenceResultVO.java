package com.maidc.model.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InferenceResultVO {

    private String requestId;

    private JsonNode results;

    private Long latencyMs;

    private String modelVersion;
}
