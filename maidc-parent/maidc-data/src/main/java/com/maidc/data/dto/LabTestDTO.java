package com.maidc.data.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 检验DTO
 */
@Data
public class LabTestDTO {
    private Long id;
    private String testNo;
    private String testType;
    private String sampleType;
    private LocalDateTime sampleTime;
    private LocalDateTime reportTime;
    private String status;
    private String orderingDoctor;
    private List<LabPanelDTO> panels;

    @Data
    public static class LabPanelDTO {
        private Long id;
        private String itemName;
        private String itemCode;
        private String result;
        private String unit;
        private String referenceRange;
        private String abnormalFlag; // NORMAL/HIGH/LOW
        private LocalDateTime reportTime;
    }
}
