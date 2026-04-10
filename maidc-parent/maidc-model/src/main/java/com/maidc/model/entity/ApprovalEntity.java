package com.maidc.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.maidc.common.jpa.base.BaseEntity;
import com.maidc.common.jpa.converter.JsonNodeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "m_approval", schema = "model")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE model.m_approval SET is_deleted = true WHERE id = ?")
public class ApprovalEntity extends BaseEntity {

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "eval_id")
    private Long evalId;

    @Column(name = "approval_type", length = 32)
    private String approvalType;

    @Column(name = "current_level")
    private Integer currentLevel = 1;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "evidence_docs", columnDefinition = "jsonb")
    private JsonNode evidenceDocs;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "risk_assessment", columnDefinition = "jsonb")
    private JsonNode riskAssessment;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "submitted_by")
    private Long submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;
}
