package com.maidc.data.repository;

import com.maidc.data.entity.DiagnosisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiagnosisRepository extends JpaRepository<DiagnosisEntity, Long>, JpaSpecificationExecutor<DiagnosisEntity> {

    List<DiagnosisEntity> findByEncounterIdAndIsDeletedFalse(Long encounterId);

    List<DiagnosisEntity> findByPatientIdAndIsDeletedFalse(Long patientId);
}
