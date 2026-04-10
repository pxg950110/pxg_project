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
public class DeploymentCreateDTO {

    private Long modelVersionId;

    @NotBlank(message = "部署名称不能为空")
    private String deploymentName;

    @NotBlank(message = "部署环境不能为空")
    private String environment;

    private JsonNode resourceConfig;

    private String endpointUrl;

    private Integer replicas;
}
