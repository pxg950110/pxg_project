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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "c_vital_sign", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_vital_sign SET is_deleted = true WHERE id = ?")
public class VitalSignEntity extends BaseEntity {

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "sign_type", nullable = false, length = 32)
    private String signType;

    @Column(name = "sign_value", precision = 8, scale = 2)
    private BigDecimal signValue;

    @Column(name = "unit", length = 16)
    private String unit;

    @Column(name = "measured_at")
    private LocalDateTime measuredAt;

    @Column(name = "measured_by", length = 32)
    private String measuredBy;
}
