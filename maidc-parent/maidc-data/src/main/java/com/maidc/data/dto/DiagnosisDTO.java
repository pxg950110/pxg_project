package com.maidc.data.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 诊断DTO
 */
@Data
public class DiagnosisDTO {
    private Long id;
    private String diagnosisType; // MAIN/SECONDARY/ADMISSION/DISCHARGE
    private String icdCode;
    private String icdName;
    private LocalDateTime diagnosisTime;
    private String doctorCode;
    private String doctorName;
    private Integer sortOrder;
}
