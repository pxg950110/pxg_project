package com.maidc.data.dto.etl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlPipelineQueryDTO {

    private String keyword;

    private Long sourceId;

    private String status;

    private String engineType;

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int pageSize = 20;
}
