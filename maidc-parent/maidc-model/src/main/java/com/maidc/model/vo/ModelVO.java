package com.maidc.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelVO {

    private Long id;

    private String modelCode;

    private String modelName;

    private String modelType;

    private String taskType;

    private String framework;

    private String status;

    private String latestVersion;

    private String tags;

    private String ownerName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
