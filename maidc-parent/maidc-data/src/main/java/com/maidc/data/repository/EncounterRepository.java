package com.maidc.data.repository;

import com.maidc.data.entity.EncounterEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EncounterRepository extends JpaRepository<EncounterEntity, Long>, JpaSpecificationExecutor<EncounterEntity> {

    Optional<EncounterEntity> findByIdAndIsDeletedFalse(Long id);

    List<EncounterEntity> findByPatientIdAndIsDeletedFalseOrderByAdmissionTimeDesc(Long patientId);

    /**
     * 按患者ID查询就诊列表(按入院时间倒序)
     */
    @Query("SELECT e FROM EncounterEntity e WHERE e.patientId = :patientId AND e.isDeleted = false ORDER BY e.admissionTime DESC")
    List<EncounterEntity> findByPatientIdOrderByAdmissionTimeDesc(@Param("patientId") Long patientId);

    /**
     * 按患者ID分页查询就诊列表
     */
    @Query("SELECT e FROM EncounterEntity e WHERE e.patientId = :patientId AND e.isDeleted = false ORDER BY e.admissionTime DESC")
    Page<EncounterEntity> findByPatientId(@Param("patientId") Long patientId, Pageable pageable);
}
