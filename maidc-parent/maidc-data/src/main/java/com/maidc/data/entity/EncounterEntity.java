package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

/**
 * 就诊记录实体，列名与 docker/init-db/04-cdr.sql 的 cdr.c_encounter DDL 严格对齐。
 * <p>字段命名即 DDL 词汇（dept_name/admit_time/doctor_name），禁止引入别名词汇
 * （department/admission_time/attending_doctor），避免 ETL 写入与应用读取列分裂。
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "c_encounter", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_encounter SET is_deleted = true WHERE id = ?")
public class EncounterEntity extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_no", nullable = false, length = 64)
    private String encounterNo;

    @Column(name = "encounter_type", length = 32)
    private String encounterType;

    @Column(name = "dept_code", length = 32)
    private String deptCode;

    @Column(name = "dept_name", length = 64)
    private String deptName;

    @Column(name = "doctor_code", length = 32)
    private String doctorCode;

    @Column(name = "doctor_name", length = 64)
    private String doctorName;

    @Column(name = "admit_time")
    private LocalDateTime admitTime;

    @Column(name = "discharge_time")
    private LocalDateTime dischargeTime;

    @Column(name = "bed_no", length = 32)
    private String bedNo;

    @Column(name = "ward_code", length = 32)
    private String wardCode;

    @Column(name = "diagnosis_code", length = 16)
    private String diagnosisCode;

    @Column(name = "diagnosis_name", length = 256)
    private String diagnosisName;

    @Column(name = "severity", length = 16)
    private String severity;

    @Column(name = "status", length = 16)
    private String status;
}
