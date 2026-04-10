package com.maidc.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCreateDTO {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 128, message = "项目名称最长128个字符")
    private String name;

    private String description;

    @NotBlank(message = "研究类型不能为空")
    private String researchType;

    private Long piId;

    private LocalDate startDate;

    private LocalDate endDate;

    private Long orgId;
}
