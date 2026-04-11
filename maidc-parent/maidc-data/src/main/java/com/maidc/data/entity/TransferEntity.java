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
@Table(name = "c_transfer", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_transfer SET is_deleted = true WHERE id = ?")
public class TransferEntity extends BaseEntity {

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "from_dept", length = 64)
    private String fromDept;

    @Column(name = "to_dept", length = 64)
    private String toDept;

    @Column(name = "transfer_type", length = 32)
    private String transferType;

    @Column(name = "transfer_time")
    private LocalDateTime transferTime;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
}
