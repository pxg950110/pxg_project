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
        name = "m_model_version",
        schema = "model",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_model_version", columnNames = {"model_id", "version_no"})
        }
)
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE model.m_model_version SET is_deleted = true WHERE id = ?")
public class ModelVersionEntity extends BaseEntity {

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    @Column(name = "version_no", nullable = false, length = 16)
    private String versionNo;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "changelog", columnDefinition = "TEXT")
    private String changelog;

    @Column(name = "framework_version", length = 32)
    private String frameworkVersion;

    @Column(name = "model_file_path", length = 256)
    private String modelFilePath;

    @Column(name = "model_file_size")
    private Long modelFileSize;

    @Column(name = "model_file_checksum", length = 64)
    private String modelFileChecksum;

    @Column(name = "config_path", length = 256)
    private String configPath;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "hyper_params", columnDefinition = "jsonb")
    private JsonNode hyperParams;

    @Column(name = "training_dataset_id")
    private Long trainingDatasetId;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "training_metrics", columnDefinition = "jsonb")
    private JsonNode trainingMetrics;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "CREATED";
}
