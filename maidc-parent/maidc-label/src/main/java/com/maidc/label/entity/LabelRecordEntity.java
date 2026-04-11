package com.maidc.label.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.maidc.common.jpa.base.BaseEntity;
import com.maidc.common.jpa.converter.JsonNodeConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "r_label_record", schema = "rdr")
public class LabelRecordEntity extends BaseEntity {

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "data_id", nullable = false, length = 36)
    private String dataId;

    @Column(name = "label", columnDefinition = "TEXT")
    private String label;

    @Column(name = "annotation", columnDefinition = "jsonb")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode annotation;

    @Column(name = "labeler_id", length = 36)
    private String labelerId;

    @Column(name = "labeler_name", length = 100)
    private String labelerName;

    @Column(name = "labeled_at")
    private LocalDateTime labeledAt;

    @Column(name = "verifier_id", length = 36)
    private String verifierId;

    @Column(name = "verifier_name", length = 100)
    private String verifierName;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_status", length = 20)
    private String verificationStatus;
}
