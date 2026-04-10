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
public class PatientDetailVO {

    private Long id;

    private String name;

    private String gender;

    private LocalDate birthDate;

    private String address;

    private Long orgId;

    private List<EncounterVO> encounters;

    private List<DiagnosisVO> diagnoses;

    private int encounterCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
