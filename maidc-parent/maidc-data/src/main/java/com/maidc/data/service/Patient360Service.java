package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.*;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.*;
import com.maidc.data.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class Patient360Service {

    private final PatientRepository patientRepository;
    private final EncounterService encounterService;
    private final DiagnosisRepository diagnosisRepository;
    private final AllergyRepository allergyRepository;
    private final LabTestRepository labTestRepository;
    private final LabPanelRepository labPanelRepository;
    private final MedicationRepository medicationRepository;
    private final ImagingExamRepository imagingExamRepository;
    private final VitalSignRepository vitalSignRepository;
    private final MicrobiologyRepository microbiologyRepository;
    private final DataMapper dataMapper;

    public PatientDetailVO getPatient360(Long patientId) {
        PatientEntity patient = patientRepository.findByIdAndIsDeletedFalse(patientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATIENT_NOT_FOUND));

        List<EncounterVO> encounters = encounterService.findByPatientId(patientId);

        List<DiagnosisVO> diagnoses = diagnosisRepository.findByPatientIdAndIsDeletedFalse(patientId)
                .stream().map(dataMapper::toDiagnosisVO).toList();

        List<AllergyVO> allergies = allergyRepository.findByPatientIdAndIsDeletedFalse(patientId)
                .stream().map(dataMapper::toAllergyVO).toList();

        List<MedicationVO> medications = medicationRepository.findByPatientIdAndIsDeletedFalse(patientId)
                .stream().map(dataMapper::toMedicationVO).toList();

        List<ImagingExamVO> imagingExams = imagingExamRepository.findByPatientIdAndIsDeletedFalse(patientId)
                .stream().map(dataMapper::toImagingExamVO).toList();

        List<VitalSignVO> vitalSigns = vitalSignRepository.findByPatientIdAndIsDeletedFalse(patientId)
                .stream().map(dataMapper::toVitalSignVO).toList();

        List<MicrobiologyVO> microbiology = microbiologyRepository.findByPatientIdAndIsDeletedFalse(patientId)
                .stream().map(dataMapper::toMicrobiologyVO).toList();

        // Lab tests with nested panel items
        List<LabTestEntity> testEntities = labTestRepository.findByPatientIdAndIsDeletedFalse(patientId);
        List<Long> testIds = testEntities.stream().map(LabTestEntity::getId).toList();
        Map<Long, List<LabPanelItemVO>> panelMap = testIds.isEmpty() ? Map.of() :
                labPanelRepository.findByTestIdInAndIsDeletedFalse(testIds)
                        .stream()
                        .map(dataMapper::toLabPanelItemVO)
                        .collect(Collectors.groupingBy(LabPanelItemVO::getTestId));

        List<LabTestVO> labTests = testEntities.stream()
                .map(dataMapper::toLabTestVO)
                .peek(vo -> vo.setItems(panelMap.getOrDefault(vo.getId(), List.of())))
                .toList();

        return PatientDetailVO.builder()
                .id(patient.getId())
                .name(patient.getName())
                .gender(patient.getGender())
                .birthDate(patient.getBirthDate())
                .address(patient.getAddress())
                .orgId(patient.getOrgId())
                .encounters(encounters)
                .diagnoses(diagnoses)
                .allergies(allergies)
                .labTests(labTests)
                .medications(medications)
                .imagingExams(imagingExams)
                .vitalSigns(vitalSigns)
                .microbiology(microbiology)
                .encounterCount(encounters.size())
                .diagnosisCount(diagnoses.size())
                .medicationCount(medications.size())
                .labTestCount(labTests.size())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .build();
    }
}
