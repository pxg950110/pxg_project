package com.maidc.data.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用药DTO
 */
@Data
public class MedicationDTO {
    private Long id;
    private String drugCode;
    private String drugName;
    private String specification;
    private String dosage;
    private String unit;
    private String frequency;
    private String usage; // PO/IV/IM
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String orderType; // LONG_TERM/TEMPORARY
    private String doctorName;
}
