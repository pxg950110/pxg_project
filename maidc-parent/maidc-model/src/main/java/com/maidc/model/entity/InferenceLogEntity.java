package com.maidc.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.maidc.common.jpa.converter.JsonNodeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "m_inference_log", schema = "model")
public class InferenceLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deployment_id", nullable = false)
    private Long deploymentId;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "input_summary", columnDefinition = "jsonb")
    private JsonNode inputSummary;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "output_result", columnDefinition = "jsonb")
    private JsonNode outputResult;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "model_version_no", length = 16)
    private String modelVersionNo;

    @Column(name = "caller_service", length = 64)
    private String callerService;

    @Column(name = "caller_user_id")
    private Long callerUserId;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "SUCCESS";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "org_id", nullable = false)
    private Long orgId;
}
