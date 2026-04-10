package com.maidc.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectQueryDTO {

    private String keyword;

    private String researchType;

    private String status;

    private Long orgId;

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int pageSize = 20;
}
