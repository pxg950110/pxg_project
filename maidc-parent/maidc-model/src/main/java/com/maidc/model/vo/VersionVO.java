package com.maidc.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionVO {

    private Long id;

    private Long modelId;

    private String versionNo;

    private String description;

    private String status;

    private Long modelFileSize;

    private String checksum;

    private LocalDateTime createdAt;
}
