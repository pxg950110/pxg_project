package com.maidc.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 路由创建/更新 DTO（前端 RouteConfig 表单：{name, type, rules[]}）。
 * <p>PUT 时 null 字段不更新。
 */
@Data
public class RouteUpsertDTO {

    private String name;

    private String type;

    private JsonNode rules;
}
