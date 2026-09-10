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
@Table(name = "c_scale_assessment", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_scale_assessment SET is_deleted = true WHERE id = ?")
public class ScaleAssessmentEntity extends BaseEntity {

    @Column(name = "followup_id", nullable = false)
    private Long followupId;
    @Column(name = "task_id")
    private Long taskId;
    @Column(name = "scale_code", nullable = false, length = 32)
    private String scaleCode;
    @Column(name = "scale_version", nullable = false)
    private Integer scaleVersion;
    @Column(name = "answers", nullable = false, columnDefinition = "jsonb")
    private String answers;
    @Column(name = "binding_sources", columnDefinition = "jsonb")
    private String bindingSources;
    @Column(name = "total_score", nullable = false)
    private Integer totalScore;
    @Column(name = "assessed_at", nullable = false)
    private LocalDateTime assessedAt;
    @Column(name = "assessed_by", nullable = false)
    private Long assessedBy;
}
