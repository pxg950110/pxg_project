package com.maidc.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataElementMappingDTO {

    private Long dataElementId;

    @NotBlank(message = "schema不能为空")
    @Size(max = 64)
    private String schemaName;

    @NotBlank(message = "表名不能为空")
    @Size(max = 128)
    private String tableName;

    @NotBlank(message = "字段名不能为空")
    @Size(max = 128)
    private String columnName;

    private String mappingType;
    private BigDecimal confidence;
    private String mappingStatus;
    private String transformRule;
}
