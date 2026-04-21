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
@Table(name = "c_disease_cohort", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_disease_cohort SET is_deleted = true WHERE id = ?")
public class DiseaseCohortEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "inclusion_rules", nullable = false, columnDefinition = "jsonb")
    private String inclusionRules;

    @Column(name = "patient_count", nullable = false)
    private Integer patientCount = 0;

    @Column(name = "auto_sync", nullable = false)
    private Boolean autoSync = true;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
}
