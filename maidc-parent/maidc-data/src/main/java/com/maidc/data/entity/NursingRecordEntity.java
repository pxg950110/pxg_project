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
@Table(name = "c_nursing_record", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_nursing_record SET is_deleted = true WHERE id = ?")
public class NursingRecordEntity extends BaseEntity {

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "record_type", length = 32)
    private String recordType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "nurse_code", length = 32)
    private String nurseCode;

    @Column(name = "record_time")
    private LocalDateTime recordTime;
}
