package com.maidc.label.vo;

import lombok.Data;

import java.util.Map;

@Data
public class LabelStatsVO {

    private String taskId;
    private Integer totalCount;
    private Integer labeledCount;
    private Integer verifiedCount;
    private Map<String, Integer> byLabel;
}
