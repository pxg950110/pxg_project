package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlPipelineDetailVO {

    private Long id;

    private String pipelineName;

    private Long sourceId;

    private String sourceName;

    private String description;

    private String engineType;

    private String status;

    private String syncMode;

    private String cronExpression;

    private LocalDateTime lastRunTime;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<EtlStepVO> steps;
}
