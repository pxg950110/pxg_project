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
@Table(name = "m_deployment", schema = "model")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE model.m_deployment SET is_deleted = true WHERE id = ?")
public class DeploymentEntity extends BaseEntity {

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "deployment_name", nullable = false, length = 128)
    private String deploymentName;

    @Column(name = "environment", length = 32)
    private String environment;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "resource_config", columnDefinition = "jsonb")
    private JsonNode resourceConfig;

    @Column(name = "endpoint_url", length = 256)
    private String endpointUrl;

    @Column(name = "replicas")
    private Integer replicas = 1;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "CREATING";

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;

    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;
}
