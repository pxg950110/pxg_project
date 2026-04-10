package com.maidc.data.vo;

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
public class EtlTaskVO {

    private Long id;

    private String name;

    private String sourceType;

    private String targetType;

    private String cronExpression;

    private String status;

    private LocalDateTime lastExecutionTime;

    private JsonNode config;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
