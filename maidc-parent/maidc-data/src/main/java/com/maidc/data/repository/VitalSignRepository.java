package com.maidc.data.repository;

import com.maidc.data.entity.VitalSignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VitalSignRepository extends JpaRepository<VitalSignEntity, Long>, JpaSpecificationExecutor<VitalSignEntity> {
    List<VitalSignEntity> findByPatientIdAndIsDeletedFalse(Long patientId);

    List<VitalSignEntity> findByEncounterIdAndIsDeletedFalse(Long encounterId);
}
