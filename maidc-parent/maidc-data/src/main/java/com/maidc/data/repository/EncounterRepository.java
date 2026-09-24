package com.maidc.data.repository;

import com.maidc.data.entity.EncounterEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EncounterRepository extends JpaRepository<EncounterEntity, Long>, JpaSpecificationExecutor<EncounterEntity> {

    Optional<EncounterEntity> findByIdAndIsDeletedFalse(Long id);

    /** 患者是否存在指定科室的就诊（DEPT 数据范围 ∃ 判定，避免全量加载就诊实体） */
    boolean existsByPatientIdAndDeptNameAndIsDeletedFalse(Long patientId, String deptName);

    List<EncounterEntity> findByPatientIdAndIsDeletedFalseOrderByAdmitTimeDesc(Long patientId);

    /**
     * 按患者ID查询就诊列表(按入院时间倒序)
     */
    @Query("SELECT e FROM EncounterEntity e WHERE e.patientId = :patientId AND e.isDeleted = false ORDER BY e.admitTime DESC")
    List<EncounterEntity> findByPatientIdOrderByAdmitTimeDesc(@Param("patientId") Long patientId);

    /**
     * 按患者ID分页查询就诊列表
     */
    @Query("SELECT e FROM EncounterEntity e WHERE e.patientId = :patientId AND e.isDeleted = false ORDER BY e.admitTime DESC")
    Page<EncounterEntity> findByPatientId(@Param("patientId") Long patientId, Pageable pageable);

    /**
     * 查询就诊详情
     * 一次性加载就诊及其关联的患者信息、诊断、检验、影像、用药、手术等数据
     */
    @Query("SELECT e FROM EncounterEntity e WHERE e.id = :encounterId AND e.isDeleted = false")
    Optional<EncounterEntity> findEncounterWithDetails(@Param("encounterId") Long encounterId);

    /**
     * 批量查询患者的所有就诊详情
     */
    @Query("SELECT e FROM EncounterEntity e WHERE e.patientId = :patientId AND e.isDeleted = false ORDER BY e.admitTime DESC")
    List<EncounterEntity> findPatientEncountersWithDetails(@Param("patientId") Long patientId);

    /**
     * 批量查询就诊 - 用于优化数据加载
     */
    @Query("SELECT e FROM EncounterEntity e WHERE e.id IN :encounterIds AND e.isDeleted = false")
    List<EncounterEntity> findEncounterByIds(@Param("encounterIds") List<Long> encounterIds);

    /**
     * 游标分页查询 - 用于大数据集场景(>50条遇诊)
     * 基于admissionTime作为游标,避免OFFSET性能问题
     */
    @Query("SELECT e FROM EncounterEntity e " +
           "WHERE e.patientId = :patientId " +
           "AND e.admitTime < :cursor " +
           "AND e.isDeleted = false " +
           "ORDER BY e.admitTime DESC")
    List<EncounterEntity> findByPatientIdWithCursor(
        @Param("patientId") Long patientId,
        @Param("cursor") LocalDateTime cursor,
        Pageable pageable);
}
