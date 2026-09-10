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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "c_treatment_record", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_treatment_record SET is_deleted = true WHERE id = ?")
public class TreatmentRecordEntity extends BaseEntity {

    @Column(name = "followup_id", nullable = false)
    private Long followupId;
    @Column(name = "task_id")
    private Long taskId;
    @Column(name = "category", nullable = false, length = 32)
    private String category;
    @Column(name = "name", nullable = false, length = 256)
    private String name;
    @Column(name = "detail", columnDefinition = "jsonb")
    private String detail;
    @Column(name = "source", nullable = false, length = 8)
    private String source;
    @Column(name = "source_ref", columnDefinition = "jsonb")
    private String sourceRef;
    @Column(name = "occurred_date", nullable = false)
    private LocalDate occurredDate;
}
