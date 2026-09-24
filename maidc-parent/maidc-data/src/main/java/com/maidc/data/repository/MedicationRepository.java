package com.maidc.data.repository;

import com.maidc.data.entity.MedicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationRepository extends JpaRepository<MedicationEntity, Long>, JpaSpecificationExecutor<MedicationEntity> {
    List<MedicationEntity> findByPatientIdAndIsDeletedFalse(Long patientId);

    List<MedicationEntity> findByEncounterIdAndIsDeletedFalse(Long encounterId);
}
