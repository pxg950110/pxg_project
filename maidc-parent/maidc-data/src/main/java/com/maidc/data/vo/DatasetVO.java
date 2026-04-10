package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetVO {

    private Long id;

    private Long projectId;

    private String name;

    private String dataType;

    private Integer versionCount;

    private Long sampleCount;

    private Long sizeBytes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
