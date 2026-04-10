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
public class RouteCreateDTO {

    @NotBlank(message = "路由名称不能为空")
    private String routeName;

    private Long modelId;

    @NotBlank(message = "路由类型不能为空")
    private String routeType;

    private JsonNode config;

    private JsonNode trafficRules;
}
