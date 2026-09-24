package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "c_patient_followup", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_patient_followup SET is_deleted = true WHERE id = ?")
public class PatientFollowupEntity extends BaseEntity {

    @Column(name = "cohort_id", nullable = false)
    private Long cohortId;
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    @Column(name = "protocol_id", nullable = false)
    private Long protocolId;
    @Column(name = "protocol_version", nullable = false)
    private Integer protocolVersion;
    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;
    @Column(name = "nurse_id")
    private Long nurseId;
    @Column(name = "status", nullable = false, length = 16)
    private String status;
    @Column(name = "enroll_date", nullable = false)
    private LocalDate enrollDate;
    @Column(name = "close_reason", length = 512)
    private String closeReason;
    @Column(name = "out_of_cohort_at")
    private LocalDateTime outOfCohortAt;

    // ---- 展示字段（不落库，列表/详情由服务层批量填充）----
    @Transient
    private String patientName;
    @Transient
    private String patientGender;
    @Transient
    private Integer patientAge;
    @Transient
    private String doctorName;
    @Transient
    private String nurseName;
}