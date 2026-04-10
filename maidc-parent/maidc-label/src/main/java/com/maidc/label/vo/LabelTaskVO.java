package com.maidc.label.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LabelTaskVO {

    private String id;
    private String name;
    private String taskType;
    private String datasetId;
    private String datasetName;
    private String assigneeId;
    private String assigneeName;
    private String status;
    private Integer totalCount;
    private Integer labeledCount;
    private Integer verifiedCount;
    private JsonNode labels;
    private LocalDateTime createdAt;
}
