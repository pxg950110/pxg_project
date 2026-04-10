package com.maidc.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionCreateDTO {

    @NotBlank(message = "版本号不能为空")
    private String versionNo;

    private String description;

    private String changelog;

    private JsonNode hyperParams;
}
