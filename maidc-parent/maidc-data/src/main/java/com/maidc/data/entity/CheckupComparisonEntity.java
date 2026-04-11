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
@Table(name = "c_checkup_comparison", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_checkup_comparison SET is_deleted = true WHERE id = ?")
public class CheckupComparisonEntity extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "current_checkup_id", nullable = false)
    private Long currentCheckupId;

    @Column(name = "previous_checkup_id", nullable = false)
    private Long previousCheckupId;

    @Column(name = "comparison_result", columnDefinition = "jsonb")
    private String comparisonResult;
}
