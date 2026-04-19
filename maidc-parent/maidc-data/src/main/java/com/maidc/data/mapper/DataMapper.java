package com.maidc.data.mapper;

import com.maidc.data.entity.*;
import com.maidc.data.vo.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DataMapper {

    // ==================== CDR ====================
    PatientVO toPatientVO(PatientEntity entity);

    EncounterVO toEncounterVO(EncounterEntity entity);

    DiagnosisVO toDiagnosisVO(DiagnosisEntity entity);

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
