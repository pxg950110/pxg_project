package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.*;
import com.maidc.data.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 就诊维度子资源查询服务。
 * <p>无外键约定下的业务层一致性校验：所有查询先确认 encounter 存在且归属指定患者
 * （不匹配时抛与"未找到"相同的异常，不暴露存在性）。
 */
@Service
@RequiredArgsConstructor
public class EncounterSubresourceService {

    private final EncounterRepository encounterRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final LabTestRepository labTestRepository;
    private final ImagingExamRepository imagingExamRepository;
    private final MedicationRepository medicationRepository;
    private final VitalSignRepository vitalSignRepository;

    /** 校验 encounter 归属患者并返回实体 */
    public EncounterEntity requireEncounterOfPatient(Long patientId, Long encounterId) {
        EncounterEntity encounter = encounterRepository.findByIdAndIsDeletedFalse(encounterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENCOUNTER_NOT_FOUND));
        if (!encounter.getPatientId().equals(patientId)) {
            throw new BusinessException(ErrorCode.ENCOUNTER_NOT_FOUND);
        }
        return encounter;
    }

    /** 就诊诊断，type 过滤 diagnosis_type（MAIN/SECONDARY/ADMISSION/DISCHARGE） */
    public List<DiagnosisEntity> diagnoses(Long patientId, Long encounterId, String type) {
        requireEncounterOfPatient(patientId, encounterId);
        List<DiagnosisEntity> all = diagnosisRepository.findByEncounterIdAndIsDeletedFalse(encounterId);
        if (type == null || type.isBlank()) {
            return all;
        }
        return all.stream()
                .filter(d -> type.equalsIgnoreCase(d.getDiagnosisType()))
                .toList();
    }

    /** 就诊检验，category 过滤 specimen_type（标本类型） */
    public List<LabTestEntity> labResults(Long patientId, Long encounterId, String category) {
        requireEncounterOfPatient(patientId, encounterId);
        List<LabTestEntity> all = labTestRepository.findByEncounterIdAndIsDeletedFalse(encounterId);
        if (category == null || category.isBlank()) {
            return all;
        }
        return all.stream()
                .filter(t -> category.equalsIgnoreCase(t.getSpecimenType()))
                .toList();
    }

    /** 就诊影像检查 */
    public List<ImagingExamEntity> imaging(Long patientId, Long encounterId) {
        requireEncounterOfPatient(patientId, encounterId);
        return imagingExamRepository.findByEncounterIdAndIsDeletedFalse(encounterId);
    }

    /** 就诊用药，status 过滤医嘱状态 */
    public List<MedicationEntity> medications(Long patientId, Long encounterId, String status) {
        requireEncounterOfPatient(patientId, encounterId);
        List<MedicationEntity> all = medicationRepository.findByEncounterIdAndIsDeletedFalse(encounterId);
        if (status == null || status.isBlank()) {
            return all;
        }
        return all.stream()
                .filter(m -> status.equalsIgnoreCase(m.getStatus()))
                .toList();
    }

    /** 就诊体征 */
    public List<VitalSignEntity> vitalSigns(Long patientId, Long encounterId) {
        requireEncounterOfPatient(patientId, encounterId);
        return vitalSignRepository.findByEncounterIdAndIsDeletedFalse(encounterId);
    }
}
