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
public class EtlStepVO {

    private Long id;

    private Long pipelineId;

    private String stepName;

    private Integer stepOrder;

    private String stepType;

    private String sourceSchema;

    private String sourceTable;

    private String targetSchema;

    private String targetTable;

    private Object joinConfig;

    private String filterCondition;

    private Object transformConfig;

    private String preSql;

    private String postSql;

    private String onError;

    private String syncMode;

    private LocalDateTime lastSyncTime;

    private String createdBy;

    private LocalDateTime createdAt;

    private List<EtlFieldMappingVO> fieldMappings;
}
