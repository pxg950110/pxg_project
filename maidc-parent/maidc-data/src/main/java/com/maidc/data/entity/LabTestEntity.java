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
@Table(name = "c_lab_test", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_lab_test SET is_deleted = true WHERE id = ?")
public class LabTestEntity extends BaseEntity {

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "test_code", length = 32)
    private String testCode;

    @Column(name = "test_name", length = 128)
    private String testName;

    @Column(name = "specimen_type", length = 32)
    private String specimenType;

    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ORDERED";

    @Column(name = "ordering_doctor", length = 32)
    private String orderingDoctor;
}
