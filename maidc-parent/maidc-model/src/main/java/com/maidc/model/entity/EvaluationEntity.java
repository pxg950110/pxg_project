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
@Table(name = "m_evaluation", schema = "model")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE model.m_evaluation SET is_deleted = true WHERE id = ?")
public class EvaluationEntity extends BaseEntity {

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "eval_name", nullable = false, length = 128)
    private String evalName;

    @Column(name = "eval_type", length = 32)
    private String evalType;

    @Column(name = "dataset_id")
    private Long datasetId;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "metrics", columnDefinition = "jsonb")
    private JsonNode metrics;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "confusion_matrix", columnDefinition = "jsonb")
    private JsonNode confusionMatrix;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "roc_data", columnDefinition = "jsonb")
    private JsonNode rocData;

    @Column(name = "report_url", length = 256)
    private String reportUrl;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
