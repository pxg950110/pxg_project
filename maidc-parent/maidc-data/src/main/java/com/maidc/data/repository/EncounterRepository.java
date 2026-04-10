package com.maidc.data.repository;

import com.maidc.data.entity.EncounterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EncounterRepository extends JpaRepository<EncounterEntity, Long>, JpaSpecificationExecutor<EncounterEntity> {

    Optional<EncounterEntity> findByIdAndIsDeletedFalse(Long id);

    List<EncounterEntity> findByPatientIdAndIsDeletedFalseOrderByAdmissionTimeDesc(Long patientId);
}
