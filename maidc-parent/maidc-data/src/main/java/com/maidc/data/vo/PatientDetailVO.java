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

    private List<AllergyVO> allergies;

    private List<LabTestVO> labTests;

    private List<MedicationVO> medications;

    private List<ImagingExamVO> imagingExams;

    private List<VitalSignVO> vitalSigns;

    private List<MicrobiologyVO> microbiology;

    private int encounterCount;

    private int diagnosisCount;

    private int medicationCount;

    private int labTestCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
