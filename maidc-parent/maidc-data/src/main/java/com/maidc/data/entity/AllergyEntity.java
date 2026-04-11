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
@Table(name = "c_allergy", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_allergy SET is_deleted = true WHERE id = ?")
public class AllergyEntity extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "allergen", length = 128)
    private String allergen;

    @Column(name = "allergen_type", length = 32)
    private String allergenType;

    @Column(name = "reaction", length = 128)
    private String reaction;

    @Column(name = "severity", length = 16)
    private String severity;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
}
