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
@Table(name = "c_medication", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_medication SET is_deleted = true WHERE id = ?")
public class MedicationEntity extends BaseEntity {

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "med_code", length = 32)
    private String medCode;

    @Column(name = "med_name", length = 128)
    private String medName;

    @Column(name = "dosage", length = 64)
    private String dosage;

    @Column(name = "route", length = 32)
    private String route;

    @Column(name = "frequency", length = 32)
    private String frequency;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "prescriber", length = 32)
    private String prescriber;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";
}
