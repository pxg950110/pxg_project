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
public class MedicationVO {
    private Long id;
    private Long encounterId;
    private String medCode;
    private String medName;
    private String dosage;
    private String route;
    private String frequency;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String prescriber;
    private String status;
}
