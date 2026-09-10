package com.maidc.data.repository;

import com.maidc.data.entity.PatientFollowupEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientFollowupRepository extends JpaRepository<PatientFollowupEntity, Long> {

    Optional<PatientFollowupEntity> findByIdAndIsDeletedFalse(Long id);

    Page<PatientFollowupEntity> findByCohortIdAndIsDeletedFalse(Long cohortId, Pageable pageable);

    Page<PatientFollowupEntity> findByCohortIdAndStatusAndIsDeletedFalse(Long cohortId, String status, Pageable pageable);

    List<PatientFollowupEntity> findByCohortIdAndStatusAndIsDeletedFalse(Long cohortId, String status);

    Optional<PatientFollowupEntity> findByCohortIdAndPatientIdAndStatusAndIsDeletedFalse(Long cohortId, Long patientId, String status);

    List<PatientFollowupEntity> findByNurseIdAndStatusAndIsDeletedFalse(Long nurseId, String status);

    List<PatientFollowupEntity> findByDoctorIdAndStatusAndIsDeletedFalse(Long doctorId, String status);

    @Query("SELECT f FROM com.maidc.data.entity.PatientFollowupEntity f WHERE f.cohortId = :cohortId AND f.isDeleted = false AND f.status = 'ACTIVE'")
    List<PatientFollowupEntity> findActiveByCohort(@Param("cohortId") Long cohortId);

    // ---- DEPT 数据范围（∃ 语义：患者任一就诊科室 == 用户科室 → 可见）----

    @Query(value = "SELECT * FROM cdr.c_patient_followup f WHERE f.is_deleted = false AND f.cohort_id = :cohortId " +
           "AND EXISTS (SELECT 1 FROM cdr.c_encounter e WHERE e.patient_id = f.patient_id AND e.department = :deptName AND e.is_deleted = false)",
           countQuery = "SELECT COUNT(*) FROM cdr.c_patient_followup f WHERE f.is_deleted = false AND f.cohort_id = :cohortId " +
           "AND EXISTS (SELECT 1 FROM cdr.c_encounter e WHERE e.patient_id = f.patient_id AND e.department = :deptName AND e.is_deleted = false)",
           nativeQuery = true)
    Page<PatientFollowupEntity> findByCohortIdAndDept(@Param("cohortId") Long cohortId, @Param("deptName") String deptName, Pageable pageable);

    @Query(value = "SELECT * FROM cdr.c_patient_followup f WHERE f.is_deleted = false AND f.cohort_id = :cohortId AND f.status = :status " +
           "AND EXISTS (SELECT 1 FROM cdr.c_encounter e WHERE e.patient_id = f.patient_id AND e.department = :deptName AND e.is_deleted = false)",
           countQuery = "SELECT COUNT(*) FROM cdr.c_patient_followup f WHERE f.is_deleted = false AND f.cohort_id = :cohortId AND f.status = :status " +
           "AND EXISTS (SELECT 1 FROM cdr.c_encounter e WHERE e.patient_id = f.patient_id AND e.department = :deptName AND e.is_deleted = false)",
           nativeQuery = true)
    Page<PatientFollowupEntity> findByCohortIdAndStatusAndDept(@Param("cohortId") Long cohortId, @Param("status") String status,
                                                               @Param("deptName") String deptName, Pageable pageable);

    @Query(value = "SELECT f.* FROM cdr.c_patient_followup f JOIN cdr.c_followup_task t ON t.followup_id = f.id AND t.is_deleted = false " +
           "WHERE f.is_deleted = false AND f.status = 'ACTIVE' AND t.status = 'PENDING' AND t.due_date <= :dueDate " +
           "AND EXISTS (SELECT 1 FROM cdr.c_encounter e WHERE e.patient_id = f.patient_id AND e.department = :deptName AND e.is_deleted = false)",
           nativeQuery = true)
    List<PatientFollowupEntity> findActiveWithPendingDueAndDept(@Param("dueDate") java.time.LocalDate dueDate,
                                                                @Param("deptName") String deptName);

    @Query(value = "SELECT f.* FROM cdr.c_patient_followup f JOIN cdr.c_followup_task t ON t.followup_id = f.id AND t.is_deleted = false " +
           "WHERE f.is_deleted = false AND f.status = 'ACTIVE' AND t.status = 'PENDING' AND t.due_date <= :dueDate",
           nativeQuery = true)
    List<PatientFollowupEntity> findActiveWithPendingDue(@Param("dueDate") java.time.LocalDate dueDate);
}