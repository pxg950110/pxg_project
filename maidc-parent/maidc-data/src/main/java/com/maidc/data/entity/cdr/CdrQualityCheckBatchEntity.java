package com.maidc.data.entity.cdr;

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
 * CDR数据质量检测批次实体
 * 记录每次质量检测的执行结果
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "cdr_quality_check_batch", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.cdr_quality_check_batch SET is_deleted = true WHERE id = ?")
public class CdrQualityCheckBatchEntity extends BaseEntity {

    @Column(name = "batch_code", nullable = false, unique = true, length = 64)
    private String batchCode;

    @Column(name = "batch_name", nullable = false, length = 128)
    private String batchName;

    @Column(name = "target_schema", nullable = false, length = 32)
    private String targetSchema;

    @Column(name = "target_table", nullable = false, length = 64)
    private String targetTable;

    @Column(name = "check_type", nullable = false, length = 32)
    private String checkType; // FULL, INCREMENTAL, SAMPLE

    @Column(name = "sample_ratio", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal sampleRatio;

    @Column(name = "check_time", nullable = false)
    private LocalDateTime checkTime;

    @Column(name = "total_records", nullable = false)
    private Long totalRecords;

    @Column(name = "passed_records", nullable = false)
    private Long passedRecords;

    @Column(name = "failed_records", nullable = false)
    private Long failedRecords;

    @Column(name = "quarantined_records")
    private Long quarantinedRecords;

    @Column(name = "pass_rate", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal passRate;

    @Column(name = "status", nullable = false, length = 16)
    private String status; // RUNNING, COMPLETED, FAILED, CANCELLED

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "triggered_by", length = 64)
    private String triggeredBy; // MANUAL, SCHEDULED, ETL

    @Column(name = "error_summary", columnDefinition = "jsonb")
    private String errorSummary; // 错误统计JSON

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}