package com.maidc.model.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationVO {

    private Long id;

    private String evalName;

    private String evalType;

    private String status;

    private JsonNode metrics;

    private JsonNode confusionMatrix;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String reportUrl;
}
