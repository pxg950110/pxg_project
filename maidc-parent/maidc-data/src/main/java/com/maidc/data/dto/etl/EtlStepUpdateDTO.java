package com.maidc.data.dto.etl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlStepUpdateDTO {

    private String stepName;

    private Integer stepOrder;

    private String stepType;

    private String sourceSchema;

    private String sourceTable;

    private String targetSchema;

    private String targetTable;

    private String joinConfig;

    private String filterCondition;

    private String transformConfig;

    private String preSql;

    private String postSql;

    private String onError;

    private String syncMode;
}
