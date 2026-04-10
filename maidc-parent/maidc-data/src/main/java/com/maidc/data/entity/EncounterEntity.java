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
@Table(name = "c_encounter", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_encounter SET is_deleted = true WHERE id = ?")
public class EncounterEntity extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_type", length = 32)
    private String encounterType;

    @Column(name = "department", length = 64)
    private String department;

    @Column(name = "admission_time")
    private LocalDateTime admissionTime;

    @Column(name = "discharge_time")
    private LocalDateTime dischargeTime;

    @Column(name = "attending_doctor", length = 64)
    private String attendingDoctor;

    @Column(name = "diagnosis_summary", columnDefinition = "TEXT")
    private String diagnosisSummary;
}
