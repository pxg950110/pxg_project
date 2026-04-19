package com.maidc.data.dto.etl;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlStepCreateDTO {

    @NotBlank(message = "步骤名称不能为空")
    @Size(max = 128, message = "步骤名称最长128个字符")
    private String stepName;

    @NotNull(message = "步骤顺序不能为空")
    private Integer stepOrder;

    @Builder.Default
    private String stepType = "ONE_TO_ONE";

    private String sourceSchema;

    @NotBlank(message = "源表不能为空")
    private String sourceTable;

    private String targetSchema;

    @NotBlank(message = "目标表不能为空")
    private String targetTable;

    private String joinConfig;

    private String filterCondition;

    private String transformConfig;

    private String preSql;

    private String postSql;

    @Builder.Default
    private String onError = "ABORT";

    @Builder.Default
    private String syncMode = "INCREMENTAL";
}
