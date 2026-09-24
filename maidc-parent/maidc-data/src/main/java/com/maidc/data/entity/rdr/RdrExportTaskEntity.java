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
 * RDR数据导出任务实体
 * 管理数据集的导出任务和状态
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "rdr_export_task", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.rdr_export_task SET is_deleted = true WHERE id = ?")
public class RdrExportTaskEntity extends BaseEntity {

    @Column(name = "task_code", nullable = false, unique = true, length = 64)
    private String taskCode;

    @Column(name = "task_name", nullable = false, length = 128)
    private String taskName;

    @Column(name = "dataset_version_id", nullable = false)
    private Long datasetVersionId;

    @Column(name = "export_format", nullable = false, length = 32)
    private String exportFormat; // CSV, JSON, PARQUET, FHIR_BUNDLE, SAS7BDAT

    @Column(name = "export_type", nullable = false, length = 32)
    private String exportType; // FULL, SAMPLE, ANONYMIZED

    @Column(name = "anonymization_level", length = 16)
    private String anonymizationLevel; // NONE, BASIC, STRICT, HIPAA_COMPLIANT

    @Column(name = "include_tables", columnDefinition = "jsonb")
    private String includeTables; // 包含的表列表JSON

    @Column(name = "exclude_columns", columnDefinition = "jsonb")
    private String excludeColumns; // 排除的列列表JSON

    @Column(name = "column_rename_rules", columnDefinition = "jsonb")
    private String columnRenameRules; // 列重命名规则JSON

    @Column(name = "output_path", length = 256)
    private String outputPath;

    @Column(name = "output_filename", length = 128)
    private String outputFilename;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "status", nullable = false, length = 16)
    private String status; // PENDING, RUNNING, SUCCESS, FAILED, CANCELLED

    @Column(name = "progress_percent", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal progressPercent;

    @Column(name = "exported_records")
    private Long exportedRecords;

    @Column(name = "exported_files")
    private Integer exportedFiles;

    @Column(name = "output_size_bytes")
    private Long outputSizeBytes;

    @Column(name = "download_url", length = 256)
    private String downloadUrl; // 下载链接

    @Column(name = "download_expire_at")
    private LocalDateTime downloadExpireAt; // 链接过期时间

    @Column(name = "download_count", nullable = false)
    private Integer downloadCount = 0;

    @Column(name = "requested_by", length = 64)
    private String requestedBy;

    @Column(name = "approved_by", length = 64)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "export_log", columnDefinition = "TEXT")
    private String exportLog;
}