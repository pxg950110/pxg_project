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
public class ModelDetailVO {

    private Long id;

    private String modelCode;

    private String modelName;

    private String description;

    private String modelType;

    private String framework;

    private JsonNode inputSchema;

    private JsonNode outputSchema;

    private String status;

    private int versionCount;

    private VersionVO latestVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
