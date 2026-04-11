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
@Table(name = "c_patient_bed", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_patient_bed SET is_deleted = true WHERE id = ?")
public class PatientBedEntity extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "bed_no", length = 16)
    private String bedNo;

    @Column(name = "ward_code", length = 32)
    private String wardCode;

    @Column(name = "building", length = 32)
    private String building;

    @Column(name = "floor", length = 16)
    private String floor;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "vacated_at")
    private LocalDateTime vacatedAt;
}
