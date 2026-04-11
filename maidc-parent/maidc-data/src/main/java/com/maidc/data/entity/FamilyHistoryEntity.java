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

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "c_family_history", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_family_history SET is_deleted = true WHERE id = ?")
public class FamilyHistoryEntity extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "relationship", length = 32)
    private String relationship;

    @Column(name = "disease_name", length = 128)
    private String diseaseName;

    @Column(name = "icd_code", length = 32)
    private String icdCode;

    @Column(name = "onset_age")
    private Integer onsetAge;
}
