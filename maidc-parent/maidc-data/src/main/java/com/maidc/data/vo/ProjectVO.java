package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectVO {

    private Long id;

    private String name;

    private String researchType;

    private Long piId;

    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    private int memberCount;

    private int datasetCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
