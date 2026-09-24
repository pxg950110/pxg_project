package com.maidc.data.dto;

import lombok.Data;
import java.util.List;

/**
 * 就诊详情DTO
 */
@Data
public class EncounterDetailDTO {
    private EncounterBasicInfoDTO basicInfo;
    private List<DiagnosisDTO> diagnoses;
    private List<LabTestDTO> labTests;
    private List<ImagingExamDTO> imagingExams;
    private List<MedicationDTO> medications;
    private List<OperationDTO> operations;
}
