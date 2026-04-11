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
@Table(name = "c_blood_transfusion", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_blood_transfusion SET is_deleted = true WHERE id = ?")
public class BloodTransfusionEntity extends BaseEntity {

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "blood_type", length = 16)
    private String bloodType;

    @Column(name = "volume_ml")
    private Integer volumeMl;

    @Column(name = "transfused_at")
    private LocalDateTime transfusedAt;

    @Column(name = "operator", length = 32)
    private String operator;

    @Column(name = "reaction_desc", columnDefinition = "TEXT")
    private String reactionDesc;
}
