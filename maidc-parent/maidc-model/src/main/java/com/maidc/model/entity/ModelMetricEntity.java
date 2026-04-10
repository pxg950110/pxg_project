package com.maidc.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "m_model_metric", schema = "model")
public class ModelMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "deployment_id")
    private Long deploymentId;

    @Column(name = "metric_type", length = 32)
    private String metricType;

    @Column(name = "metric_name", length = 64)
    private String metricName;

    @Column(name = "metric_value", precision = 12, scale = 4)
    private BigDecimal metricValue;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "org_id", nullable = false)
    private Long orgId;
}
