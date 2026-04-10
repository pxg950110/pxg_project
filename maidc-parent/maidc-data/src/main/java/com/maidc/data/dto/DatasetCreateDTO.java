package com.maidc.data.dto;

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
public class DatasetCreateDTO {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @NotBlank(message = "数据集名称不能为空")
    @Size(max = 128, message = "数据集名称最长128个字符")
    private String name;

    private String description;

    @NotBlank(message = "数据类型不能为空")
    private String dataType;

    private Long orgId;
}
