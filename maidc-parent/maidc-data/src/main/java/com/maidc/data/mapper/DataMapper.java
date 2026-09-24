package com.maidc.data.mapper;

import com.maidc.data.entity.*;
import com.maidc.data.vo.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DataMapper {

    // ==================== CDR ====================
    PatientVO toPatientVO(PatientEntity entity);

    /**
     * 实体字段已对齐 DDL 词汇（dept_name/admit_time/doctor_name/diagnosis_name），
     * VO 对前端契约保持旧键名（department/admissionTime/attendingDoctor/diagnosisSummary），
     * 此处显式桥接，防止 MapStruct 同名静默映射失败。
     */
    @Mapping(source = "deptName", target = "department")
    @Mapping(source = "admitTime", target = "admissionTime")
    @Mapping(source = "doctorName", target = "attendingDoctor")
    @Mapping(source = "diagnosisName", target = "diagnosisSummary")
    EncounterVO toEncounterVO(EncounterEntity entity);

    DiagnosisVO toDiagnosisVO(DiagnosisEntity entity);

    AllergyVO toAllergyVO(AllergyEntity entity);

    LabTestVO toLabTestVO(LabTestEntity entity);

    LabPanelItemVO toLabPanelItemVO(LabPanelEntity entity);

    MedicationVO toMedicationVO(MedicationEntity entity);

    ImagingExamVO toImagingExamVO(ImagingExamEntity entity);

    VitalSignVO toVitalSignVO(VitalSignEntity entity);

    MicrobiologyVO toMicrobiologyVO(MicrobiologyEntity entity);

    // ==================== RDR ====================
    ProjectVO toProjectVO(ProjectEntity entity);

    DatasetVO toDatasetVO(DatasetEntity entity);

    EtlTaskVO toEtlTaskVO(EtlTaskEntity entity);

    // ==================== Dataset Version ====================
    DatasetDetailVO.DatasetVersionVO toDatasetVersionVO(DatasetVersionEntity entity);

    // ==================== Project Member ====================
    ProjectDetailVO.ProjectMemberVO toProjectMemberVO(ProjectMemberEntity entity);

    // ==================== ETL Pipeline ====================
    EtlPipelineVO toEtlPipelineVO(EtlPipelineEntity entity);

    EtlStepVO toEtlStepVO(EtlStepEntity entity);

    EtlFieldMappingVO toEtlFieldMappingVO(EtlFieldMappingEntity entity);

    EtlExecutionVO toEtlExecutionVO(EtlExecutionEntity entity);
}
