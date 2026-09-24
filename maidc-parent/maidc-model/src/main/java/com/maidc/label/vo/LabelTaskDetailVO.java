package com.maidc.label.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LabelTaskDetailVO extends LabelTaskVO {

    private String guidelines;
    private JsonNode config;
}
