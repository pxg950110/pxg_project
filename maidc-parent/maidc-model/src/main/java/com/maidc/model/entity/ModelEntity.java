package com.maidc.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.maidc.common.jpa.base.BaseEntity;
import com.maidc.common.jpa.converter.JsonNodeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(
        name = "m_model",
        schema = "model",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_model_org_code", columnNames = {"org_id", "model_code"})
        }
)
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE model.m_model SET is_deleted = true WHERE id = ?")
public class ModelEntity extends BaseEntity {

    @Column(name = "model_code", nullable = false, length = 32)
    private String modelCode;

    @Column(name = "model_name", nullable = false, length = 128)
    private String modelName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "model_type", length = 32)
    private String modelType;

    @Column(name = "task_type", length = 32)
    private String taskType;

    @Column(name = "framework", length = 32)
    private String framework;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "input_schema", columnDefinition = "jsonb")
    private JsonNode inputSchema;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "output_schema", columnDefinition = "jsonb")
    private JsonNode outputSchema;

    @Column(name = "tags", length = 256)
    private String tags;

    @Column(name = "license", length = 32)
    private String license;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "DRAFT";
}
