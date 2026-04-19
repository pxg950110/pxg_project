package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlExecutionVO {

    private Long id;

    private Long pipelineId;

    private String pipelineName;

    private Long stepId;

    private String stepName;

    private String status;

    private String engineConfig;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long rowsRead;

    private Long rowsWritten;

    private Long rowsSkipped;

    private Long rowsError;

    private String errorMessage;

    private String logPath;

    private String triggerType;

    private Object executionSnapshot;

    private String createdBy;

    private LocalDateTime createdAt;
}
