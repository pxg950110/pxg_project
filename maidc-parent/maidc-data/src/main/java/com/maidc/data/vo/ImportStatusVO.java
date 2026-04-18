package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportStatusVO {

    private String batchId;
    private int total;
    private int success;
    private int failed;
    private int running;
    private int pending;
    private List<TableStatus> tables;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableStatus {
        private String table;
        private String status;
        private Long csvRows;
        private Long dbRows;
        private Boolean match;
        private Integer duration;
        private String error;
        private String startedAt;
        private String finishedAt;
    }
}
