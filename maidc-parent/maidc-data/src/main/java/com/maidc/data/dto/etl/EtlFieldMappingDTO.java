package com.maidc.data.dto.etl;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlFieldMappingDTO {

    private Long id;

    private String sourceColumn;

    private String sourceTableAlias;

    @NotBlank(message = "目标字段不能为空")
    private String targetColumn;

    @Builder.Default
    private String transformType = "DIRECT";

    private String transformExpr;

    private String defaultValue;

    @Builder.Default
    private Boolean isRequired = false;

    private Integer sortOrder;
}
