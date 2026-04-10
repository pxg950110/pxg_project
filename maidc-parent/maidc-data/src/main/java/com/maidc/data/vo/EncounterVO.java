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
public class EncounterVO {

    private Long id;

    private Long patientId;

    private String encounterType;

    private String department;

    private LocalDateTime admissionTime;

    private LocalDateTime dischargeTime;

    private String attendingDoctor;

    private String diagnosisSummary;

    private LocalDateTime createdAt;
}
