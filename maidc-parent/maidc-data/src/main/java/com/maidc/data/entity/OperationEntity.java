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
@Table(name = "c_operation", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_operation SET is_deleted = true WHERE id = ?")
public class OperationEntity extends BaseEntity {

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "operation_code", length = 32)
    private String operationCode;

    @Column(name = "operation_name", length = 128)
    private String operationName;

    @Column(name = "operated_at")
    private LocalDateTime operatedAt;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(name = "surgeon", length = 32)
    private String surgeon;

    @Column(name = "assistant", length = 64)
    private String assistant;

    @Column(name = "anesthesia_type", length = 32)
    private String anesthesiaType;
}
