package com.maidc.data.dto.etl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlExecutionQueryDTO {

    private Long pipelineId;

    private Long stepId;

    private String status;

    private String triggerType;

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int pageSize = 20;
}
