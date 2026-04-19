package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlFieldMappingVO {

    private Long id;

    private Long stepId;

    private String sourceColumn;

    private String sourceTableAlias;

    private String targetColumn;

    private String transformType;

    private String transformExpr;

    private String defaultValue;

    private Boolean isRequired;

    private Integer sortOrder;
}
