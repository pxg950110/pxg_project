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
@Table(name = "c_pathology", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_pathology SET is_deleted = true WHERE id = ?")
public class PathologyEntity extends BaseEntity {

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "specimen_no", length = 32)
    private String specimenNo;

    @Column(name = "specimen_type", length = 32)
    private String specimenType;

    @Column(name = "diagnosis_desc", columnDefinition = "TEXT")
    private String diagnosisDesc;

    @Column(name = "grade", length = 16)
    private String grade;

    @Column(name = "stage", length = 16)
    private String stage;

    @Column(name = "report_date")
    private LocalDateTime reportDate;
}
