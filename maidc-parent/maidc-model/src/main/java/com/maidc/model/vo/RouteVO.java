package com.maidc.model.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteVO {

    private Long id;

    private String routeName;

    private String routeType;

    private JsonNode config;

    private String status;
}
