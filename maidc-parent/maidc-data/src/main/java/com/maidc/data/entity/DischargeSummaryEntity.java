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

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "c_discharge_summary", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_discharge_summary SET is_deleted = true WHERE id = ?")
public class DischargeSummaryEntity extends BaseEntity {

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "admission_diagnosis", columnDefinition = "TEXT")
    private String admissionDiagnosis;

    @Column(name = "discharge_diagnosis", columnDefinition = "TEXT")
    private String dischargeDiagnosis;

    @Column(name = "treatment_summary", columnDefinition = "TEXT")
    private String treatmentSummary;

    @Column(name = "discharge_instruction", columnDefinition = "TEXT")
    private String dischargeInstruction;

    @Column(name = "follow_up_plan", columnDefinition = "TEXT")
    private String followUpPlan;

    @Column(name = "discharged_at")
    private LocalDateTime dischargedAt;

    @Column(name = "discharge_doctor", length = 32)
    private String dischargeDoctor;
}
