package com.maidc.data.repository;

import com.maidc.data.entity.LabTestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabTestRepository extends JpaRepository<LabTestEntity, Long>, JpaSpecificationExecutor<LabTestEntity> {
    List<LabTestEntity> findByPatientIdAndIsDeletedFalse(Long patientId);

    List<LabTestEntity> findByEncounterIdAndIsDeletedFalse(Long encounterId);
}
