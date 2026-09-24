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
 * RDR抽取任务执行记录实体
 * 记录每次抽取任务的执行详情
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "rdr_extraction_execution", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.rdr_extraction_execution SET is_deleted = true WHERE id = ?")
public class RdrExtractionExecutionEntity extends BaseEntity {

    @Column(name = "execution_code", nullable = false, unique = true, length = 64)
    private String executionCode;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "execution_type", nullable = false, length = 32)
    private String executionType; // MANUAL, SCHEDULED, RETRY

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "status", nullable = false, length = 16)
    private String status; // RUNNING, SUCCESS, FAILED, CANCELLED, TIMEOUT

    @Column(name = "progress_percent", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal progressPercent;

    @Column(name = "current_step", length = 64)
    private String currentStep;

    @Column(name = "source_record_count")
    private Long sourceRecordCount;

    @Column(name = "extracted_patient_count")
    private Long extractedPatientCount;

    @Column(name = "extracted_record_count")
    private Long extractedRecordCount;

    @Column(name = "output_table_name", length = 64)
    private String outputTableName;

    @Column(name = "output_path", length = 256)
    private String outputPath; // 文件输出路径

    @Column(name = "output_size_bytes")
    private Long outputSizeBytes;

    @Column(name = "dataset_version_id")
    private Long datasetVersionId;

    @Column(name = "triggered_by", length = 64)
    private String triggeredBy;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack", columnDefinition = "TEXT")
    private String errorStack;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "parent_execution_id")
    private Long parentExecutionId; // 重试时的父执行ID

    @Column(name = "execution_log", columnDefinition = "TEXT")
    private String executionLog;

    @Column(name = "statistics", columnDefinition = "jsonb")
    private String statistics; // 执行统计JSON
}