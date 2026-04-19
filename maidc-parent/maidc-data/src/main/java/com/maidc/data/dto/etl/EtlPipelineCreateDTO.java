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
public class EtlPipelineCreateDTO {

    @NotBlank(message = "流水线名称不能为空")
    @Size(max = 128, message = "流水线名称最长128个字符")
    private String pipelineName;

    @NotNull(message = "数据源ID不能为空")
    private Long sourceId;

    private String description;

    @Builder.Default
    private String engineType = "EMBULK";

    @Builder.Default
    private String syncMode = "MANUAL";

    private String cronExpression;
}
