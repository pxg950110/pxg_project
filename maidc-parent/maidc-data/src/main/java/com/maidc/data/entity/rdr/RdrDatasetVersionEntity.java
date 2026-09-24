package com.maidc.data.entity.rdr;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

/**
 * RDR数据集版本实体
 * 管理科研数据集的版本和历史
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "rdr_dataset_version", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.rdr_dataset_version SET is_deleted = true WHERE id = ?")
public class RdrDatasetVersionEntity extends BaseEntity {

    @Column(name = "version_code", nullable = false, unique = true, length = 64)
    private String versionCode;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "version_number", nullable = false, length = 16)
    private String versionNumber; // v1.0, v1.1, v2.0

    @Column(name = "version_name", nullable = false, length = 128)
    private String versionName;

    @Column(name = "extraction_execution_id")
    private Long extractionExecutionId; // 关联的抽取执行ID

    @Column(name = "cohort_id")
    private Long cohortId; // 关联的队列ID

    @Column(name = "patient_count", nullable = false)
    private Long patientCount;

    @Column(name = "record_count", nullable = false)
    private Long recordCount;

    @Column(name = "table_count", nullable = false)
    private Integer tableCount;

    @Column(name = "storage_size_bytes")
    private Long storageSizeBytes;

    @Column(name = "schema_definition", columnDefinition = "jsonb")
    private String schemaDefinition; // 数据集Schema定义JSON

    @Column(name = "quality_report", columnDefinition = "jsonb")
    private String qualityReport; // 数据质量报告JSON

    @Column(name = "lineage_info", columnDefinition = "jsonb")
    private String lineageInfo; // 数据血缘信息JSON

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "status", nullable = false, length = 16)
    private String status; // DRAFT, PUBLISHED, ARCHIVED, DEPRECATED

    @Column(name = "is_current")
    private Boolean isCurrent = false; // 是否当前版本

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary; // 版本变更说明

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "published_by", length = 64)
    private String publishedBy;

    @Column(name = "access_count", nullable = false)
    private Integer accessCount = 0;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "retention_days")
    private Integer retentionDays; // 数据保留天数

    @Column(name = "expire_at")
    private LocalDateTime expireAt; // 过期时间
}