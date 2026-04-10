package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetDetailVO {

    private Long id;

    private Long projectId;

    private String name;

    private String description;

    private String dataType;

    private Integer versionCount;

    private Long sampleCount;

    private Long sizeBytes;

    private List<DatasetVersionVO> versions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetVersionVO {
        private Long id;
        private String versionNo;
        private String filePath;
        private String checksum;
        private Long sampleCount;
        private LocalDateTime createdAt;
    }
}
