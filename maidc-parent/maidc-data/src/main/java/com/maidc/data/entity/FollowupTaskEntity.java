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
@Table(name = "c_followup_task", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_followup_task SET is_deleted = true WHERE id = ?")
public class FollowupTaskEntity extends BaseEntity {

    @Column(name = "followup_id", nullable = false)
    private Long followupId;
    @Column(name = "stage_code", nullable = false, length = 32)
    private String stageCode;
    @Column(name = "stage_name", length = 64)
    private String stageName;
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    @Column(name = "status", nullable = false, length = 16)
    private String status;
    @Column(name = "required_scales", columnDefinition = "jsonb")
    private String requiredScales;
    @Column(name = "optional_scales", columnDefinition = "jsonb")
    private String optionalScales;
    @Column(name = "skip_reason", length = 512)
    private String skipReason;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "completed_by")
    private Long completedBy;
}
