package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailVO {

    private Long id;

    private String name;

    private String description;

    private String researchType;

    private Long piId;

    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    private List<ProjectMemberVO> members;

    private List<DatasetVO> datasets;

    private int memberCount;

    private int datasetCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectMemberVO {
        private Long id;
        private Long userId;
        private String role;
        private LocalDateTime joinedAt;
    }
}
