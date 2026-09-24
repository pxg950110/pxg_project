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
public class LabTestVO {
    private Long id;
    private Long encounterId;
    private String testCode;
    private String testName;
    private String specimenType;
    private LocalDateTime orderedAt;
    private LocalDateTime collectedAt;
    private LocalDateTime reportedAt;
    private String status;
    private String orderingDoctor;
    private List<LabPanelItemVO> items;
}
