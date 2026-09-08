package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.security.context.CurrentUser;
import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.scope.DataScopeHelper;
import com.maidc.common.security.store.PermissionStore;
import com.maidc.data.dto.*;
import com.maidc.data.entity.*;
import com.maidc.data.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 患者就诊服务 - 患者就诊360视图
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientEncounterService {

    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final LabTestRepository labTestRepository;
    private final LabPanelRepository labPanelRepository;
    private final ImagingExamRepository imagingExamRepository;
    private final MedicationRepository medicationRepository;
    private final OperationRepository operationRepository;
    private final PermissionStore permissionStore;
    private final InstitutionRepository institutionRepository;

    /**
     * 获取患者就诊列表
     */
    public PatientEncounterListDTO getPatientEncounterList(Long patientId, Pageable pageable) {
        // 获取患者信息
        PatientEntity patient = patientRepository.findByIdAndIsDeletedFalse(patientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATIENT_NOT_FOUND));

        // DEPT 数据范围校验（∃ 语义）：至少一条就诊科室 == 用户科室 → 可见；零就诊/无匹配 → 与患者未找到同响应
        checkDeptScope(encounterRepository.findByPatientIdAndIsDeletedFalseOrderByAdmissionTimeDesc(patientId)
                .stream().map(EncounterEntity::getDepartment).toList(), ErrorCode.PATIENT_NOT_FOUND);

        // 获取就诊列表（分页）
        Page<EncounterEntity> encounterPage = encounterRepository.findByPatientId(patientId, pageable);
        List<EncounterEntity> encounters = encounterPage.getContent();

        // 获取所有就诊的主诊断
        List<Long> encounterIds = encounters.stream().map(EncounterEntity::getId).toList();
        Map<Long, String> mainDiagnosisMap = getMainDiagnosisMap(encounterIds);

        // 确定当前就诊（最新或状态为ACTIVE）
        Long currentEncounterId = determineCurrentEncounter(encounters);

        // 构建时间轴DTO列表
        List<EncounterTimelineDTO> timelineList = encounters.stream()
                .map(e -> convertToTimelineDTO(e, mainDiagnosisMap.get(e.getId()), currentEncounterId))
                .toList();

        // 构建返回DTO
        PatientEncounterListDTO dto = new PatientEncounterListDTO();
        dto.setPatientId(patient.getId());
        dto.setPatientName(desensitizeName(patient.getName()));
        dto.setGender(patient.getGender());
        dto.setAge(calculateAge(patient.getBirthDate()));
        dto.setIdCard(desensitizeIdCard(patient.getIdCardHash()));
        dto.setPhone(desensitizePhone(patient.getPhoneHash()));
        dto.setEncounters(timelineList);

        return dto;
    }

    /**
     * 获取就诊详情
     */
    public EncounterDetailDTO getEncounterDetail(Long encounterId) {
        // 获取就诊基本信息
        EncounterEntity encounter = encounterRepository.findByIdAndIsDeletedFalse(encounterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENCOUNTER_NOT_FOUND));

        // DEPT 数据范围校验：就诊科室 ∉ 用户科室 → 与就诊未找到同响应（fail-closed，不暴露存在性）
        checkDeptScope(Collections.singletonList(encounter.getDepartment()), ErrorCode.ENCOUNTER_NOT_FOUND);

        // 获取主诊断
        List<DiagnosisEntity> diagnoses = diagnosisRepository.findByEncounterIdAndIsDeletedFalse(encounterId);
        String mainDiagnosis = diagnoses.isEmpty() ? null : diagnoses.get(0).getDiagnosisName();

        // 构建基本信息DTO
        EncounterBasicInfoDTO basicInfo = convertToBasicInfoDTO(encounter, mainDiagnosis);

        // 获取诊断列表
        List<DiagnosisDTO> diagnosisDTOs = diagnoses.stream()
                .map(this::convertToDiagnosisDTO)
                .toList();

        // 获取检验列表（含面板）
        List<LabTestDTO> labTestDTOs = getLabTestDTOs(encounterId);

        // 获取影像检查列表
        List<ImagingExamDTO> imagingExamDTOs = imagingExamRepository.findByEncounterIdAndIsDeletedFalse(encounterId)
                .stream()
                .map(this::convertToImagingExamDTO)
                .toList();

        // 获取用药列表
        List<MedicationDTO> medicationDTOs = medicationRepository.findByEncounterIdAndIsDeletedFalse(encounterId)
                .stream()
                .map(this::convertToMedicationDTO)
                .toList();

        // 获取手术列表
        List<OperationDTO> operationDTOs = operationRepository.findByEncounterIdAndIsDeletedFalse(encounterId)
                .stream()
                .map(this::convertToOperationDTO)
                .toList();

        // 构建返回DTO
        EncounterDetailDTO dto = new EncounterDetailDTO();
        dto.setBasicInfo(basicInfo);
        dto.setDiagnoses(diagnosisDTOs);
        dto.setLabTests(labTestDTOs);
        dto.setImagingExams(imagingExamDTOs);
        dto.setMedications(medicationDTOs);
        dto.setOperations(operationDTOs);

        return dto;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * DEPT 数据范围校验（fail-closed，∃ 语义）：至少一条就诊科室与用户科室匹配 → 可见；
     * 零就诊记录或全部不匹配 → 抛与真实"未找到"完全相同的异常（同 code 同 message，不暴露存在性）。
     * <p>s_user.dept_id 指向机构表ID，而 c_encounter.department 存科室名称，
     * 需经机构表 name 转换后按字符串比对；null 科室不计为匹配。
     */
    private void checkDeptScope(List<String> departments, ErrorCode notFound) {
        Long userId = CurrentUser.userId();
        if (userId == null) {
            // 无用户上下文（内部调用），认证与接口级权限由网关/权限切面负责
            return;
        }
        PermissionContext ctx = permissionStore.load(userId);
        if (!DataScopeHelper.needDeptFilter(ctx)) {
            return;
        }
        Long deptId = DataScopeHelper.deptId(ctx);
        String deptName = deptId == null ? null
                : institutionRepository.findById(deptId).map(InstitutionEntity::getName).orElse(null);
        // deptId 为空 / 机构不存在 / 无任何匹配科室（含零就诊、null 科室）：宁可拒绝不可放行
        if (deptName == null || departments.stream().noneMatch(dept -> deptName.equals(dept))) {
            throw new BusinessException(notFound);
        }
    }

    /**
     * 获取主诊断映射（就诊ID -> 主诊断名称）
     */
    private Map<Long, String> getMainDiagnosisMap(List<Long> encounterIds) {
        if (encounterIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 批量获取所有诊断，按encounterId分组
        return encounterIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            List<DiagnosisEntity> diagnoses = diagnosisRepository.findByEncounterIdAndIsDeletedFalse(id);
                            return diagnoses.isEmpty() ? null : diagnoses.get(0).getDiagnosisName();
                        }
                ));
    }

    /**
     * 确定当前就诊ID
     */
    private Long determineCurrentEncounter(List<EncounterEntity> encounters) {
        if (encounters.isEmpty()) {
            return null;
        }

        // 优先找状态为ACTIVE的就诊（这里假设dischargeTime为空表示在院）
        return encounters.stream()
                .filter(e -> e.getDischargeTime() == null)
                .findFirst()
                .map(EncounterEntity::getId)
                .orElse(encounters.get(0).getId()); // 否则返回最新的
    }

    /**
     * 转换为时间轴DTO
     */
    private EncounterTimelineDTO convertToTimelineDTO(EncounterEntity entity, String mainDiagnosis, Long currentEncounterId) {
        EncounterTimelineDTO dto = new EncounterTimelineDTO();
        dto.setEncounterId(entity.getId());
        dto.setEncounterNo(String.valueOf(entity.getId())); // 使用ID作为就诊号
        dto.setEncounterType(entity.getEncounterType());
        dto.setDeptCode(entity.getDepartment());
        dto.setDeptName(entity.getDepartment()); // 暂时使用department字段
        dto.setAdmitTime(entity.getAdmissionTime());
        dto.setDischargeTime(entity.getDischargeTime());
        dto.setMainDiagnosis(mainDiagnosis);
        dto.setStatus(entity.getDischargeTime() == null ? "ACTIVE" : "DISCHARGED");
        dto.setIsCurrent(entity.getId().equals(currentEncounterId));
        return dto;
    }

    /**
     * 转换为基本信息DTO
     */
    private EncounterBasicInfoDTO convertToBasicInfoDTO(EncounterEntity entity, String mainDiagnosis) {
        EncounterBasicInfoDTO dto = new EncounterBasicInfoDTO();
        dto.setEncounterId(entity.getId());
        dto.setEncounterNo(String.valueOf(entity.getId()));
        dto.setEncounterType(entity.getEncounterType());
        dto.setDeptCode(entity.getDepartment());
        dto.setDeptName(entity.getDepartment());
        dto.setDoctorCode(entity.getAttendingDoctor());
        dto.setDoctorName(entity.getAttendingDoctor());
        dto.setAdmitTime(entity.getAdmissionTime());
        dto.setDischargeTime(entity.getDischargeTime());
        dto.setMainDiagnosis(mainDiagnosis);
        dto.setStatus(entity.getDischargeTime() == null ? "ACTIVE" : "DISCHARGED");
        return dto;
    }

    /**
     * 转换为诊断DTO
     */
    private DiagnosisDTO convertToDiagnosisDTO(DiagnosisEntity entity) {
        DiagnosisDTO dto = new DiagnosisDTO();
        dto.setId(entity.getId());
        dto.setDiagnosisType(entity.getDiagnosisType());
        dto.setIcdCode(entity.getDiagnosisCode());
        dto.setIcdName(entity.getDiagnosisName());
        return dto;
    }

    /**
     * 获取检验DTO列表（含面板）
     */
    private List<LabTestDTO> getLabTestDTOs(Long encounterId) {
        List<LabTestEntity> labTests = labTestRepository.findByEncounterIdAndIsDeletedFalse(encounterId);
        if (labTests.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取所有检验的面板
        List<Long> testIds = labTests.stream().map(LabTestEntity::getId).toList();
        Map<Long, List<LabPanelEntity>> panelMap = labPanelRepository.findByTestIdInAndIsDeletedFalse(testIds)
                .stream()
                .collect(Collectors.groupingBy(LabPanelEntity::getTestId));

        return labTests.stream()
                .map(test -> convertToLabTestDTO(test, panelMap.getOrDefault(test.getId(), Collections.emptyList())))
                .toList();
    }

    /**
     * 转换为检验DTO
     */
    private LabTestDTO convertToLabTestDTO(LabTestEntity entity, List<LabPanelEntity> panels) {
        LabTestDTO dto = new LabTestDTO();
        dto.setId(entity.getId());
        dto.setTestNo(String.valueOf(entity.getId()));
        dto.setTestType(entity.getTestName());
        dto.setSampleType(entity.getSpecimenType());
        dto.setSampleTime(entity.getCollectedAt());
        dto.setReportTime(entity.getReportedAt());
        dto.setStatus(entity.getStatus());
        dto.setOrderingDoctor(entity.getOrderingDoctor());

        // 转换面板
        List<LabTestDTO.LabPanelDTO> panelDTOs = panels.stream()
                .map(this::convertToLabPanelDTO)
                .toList();
        dto.setPanels(panelDTOs);

        return dto;
    }

    /**
     * 转换为检验面板DTO
     */
    private LabTestDTO.LabPanelDTO convertToLabPanelDTO(LabPanelEntity entity) {
        LabTestDTO.LabPanelDTO dto = new LabTestDTO.LabPanelDTO();
        dto.setId(entity.getId());
        dto.setItemName(entity.getItemName());
        dto.setItemCode(entity.getItemCode());
        dto.setResult(entity.getResultValue());
        dto.setUnit(entity.getResultUnit());
        dto.setReferenceRange(entity.getReferenceRange());
        dto.setAbnormalFlag(entity.getAbnormalFlag() ? "ABNORMAL" : "NORMAL");
        return dto;
    }

    /**
     * 转换为影像检查DTO
     */
    private ImagingExamDTO convertToImagingExamDTO(ImagingExamEntity entity) {
        ImagingExamDTO dto = new ImagingExamDTO();
        dto.setId(entity.getId());
        dto.setExamNo(entity.getAccessionNo());
        dto.setExamType(entity.getExamType());
        dto.setBodyPart(entity.getBodyPart());
        dto.setModality(entity.getModality());
        dto.setExamTime(entity.getStudyDate());
        dto.setStatus(entity.getStatus());
        dto.setFindings(entity.getReportText());
        return dto;
    }

    /**
     * 转换为用药DTO
     */
    private MedicationDTO convertToMedicationDTO(MedicationEntity entity) {
        MedicationDTO dto = new MedicationDTO();
        dto.setId(entity.getId());
        dto.setDrugCode(entity.getMedCode());
        dto.setDrugName(entity.getMedName());
        dto.setDosage(entity.getDosage());
        dto.setFrequency(entity.getFrequency());
        dto.setUsage(entity.getRoute());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setDoctorName(entity.getPrescriber());
        return dto;
    }

    /**
     * 转换为手术DTO
     */
    private OperationDTO convertToOperationDTO(OperationEntity entity) {
        OperationDTO dto = new OperationDTO();
        dto.setId(entity.getId());
        dto.setOperationNo(String.valueOf(entity.getId()));
        dto.setOperationName(entity.getOperationName());
        dto.setOperationCode(entity.getOperationCode());
        dto.setStartTime(entity.getOperatedAt());
        dto.setSurgeonName(entity.getSurgeon());
        dto.setAssistantDoctor(entity.getAssistant());
        dto.setAnesthesiaMethod(entity.getAnesthesiaType());
        return dto;
    }

    /**
     * 姓名脱敏 - 张**
     */
    private String desensitizeName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (name.length() == 1) {
            return name + "**";
        }
        return name.charAt(0) + "**";
    }

    /**
     * 身份证号脱敏 - 320***********1234
     */
    private String desensitizeIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return null;
        }
        // 假设idCardHash存储的是脱敏后的值或原始值
        // 这里简单处理，实际应根据业务需求调整
        int len = idCard.length();
        return idCard.substring(0, 3) + "***********" + idCard.substring(len - 4);
    }

    /**
     * 手机号脱敏 - 138****5678
     */
    private String desensitizePhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return null;
        }
        int len = phone.length();
        return phone.substring(0, 3) + "****" + phone.substring(len - 4);
    }

    /**
     * 根据出生日期计算年龄
     */
    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
