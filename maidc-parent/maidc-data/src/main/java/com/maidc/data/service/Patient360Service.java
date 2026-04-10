package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.DiagnosisEntity;
import com.maidc.data.entity.PatientEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.DiagnosisRepository;
import com.maidc.data.repository.PatientRepository;
import com.maidc.data.vo.DiagnosisVO;
import com.maidc.data.vo.EncounterVO;
import com.maidc.data.vo.PatientDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class Patient360Service {

    private final PatientRepository patientRepository;
    private final EncounterService encounterService;
    private final DiagnosisRepository diagnosisRepository;
    private final DataMapper dataMapper;

    /**
     * Get patient 360-degree view: patient + encounters + diagnoses
     */
    public PatientDetailVO getPatient360(Long patientId) {
        PatientEntity patient = patientRepository.findByIdAndIsDeletedFalse(patientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATIENT_NOT_FOUND));

        List<EncounterVO> encounters = encounterService.findByPatientId(patientId);

        List<DiagnosisVO> diagnoses = diagnosisRepository.findByPatientIdAndIsDeletedFalse(patientId)
                .stream()
                .map(dataMapper::toDiagnosisVO)
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
                .encounterCount(encounters.size())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .build();
    }
}
