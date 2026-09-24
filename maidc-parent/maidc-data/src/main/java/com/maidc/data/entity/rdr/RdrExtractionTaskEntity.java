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

/**
 * RDR数据抽取任务实体
 * 定义从CDR抽取数据到RDR的任务配置
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "rdr_extraction_task", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.rdr_extraction_task SET is_deleted = true WHERE id = ?")
public class RdrExtractionTaskEntity extends BaseEntity {

    @Column(name = "task_code", nullable = false, unique = true, length = 64)
    private String taskCode;

    @Column(name = "task_name", nullable = false, length = 128)
    private String taskName;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "extraction_type", nullable = false, length = 32)
    private String extractionType; // CONDITION_BASED, COHORT_BASED, FULL_TABLE

    @Column(name = "source_schema", nullable = false, length = 32)
    private String sourceSchema; // cdr

    @Column(name = "source_tables", columnDefinition = "jsonb")
    private String sourceTables; // 源表列表JSON

    @Column(name = "join_strategy", length = 32)
    private String joinStrategy; // PATIENT_CENTERED, ENCOUNTER_CENTERED, TIME_WINDOW

    @Column(name = "inclusion_criteria", columnDefinition = "jsonb")
    private String inclusionCriteria; // 纳入标准JSON

    @Column(name = "exclusion_criteria", columnDefinition = "jsonb")
    private String exclusionCriteria; // 排除标准JSON

    @Column(name = "time_range_start")
    private java.time.LocalDate timeRangeStart;

    @Column(name = "time_range_end")
    private java.time.LocalDate timeRangeEnd;

    @Column(name = "output_format", nullable = false, length = 32)
    private String outputFormat; // TABLE, CSV, PARQUET, FHIR

    @Column(name = "output_schema", length = 32)
    private String outputSchema; // rdr

    @Column(name = "output_table_prefix", length = 32)
    private String outputTablePrefix;

    @Column(name = "schedule_type", length = 32)
    private String scheduleType; // MANUAL, CRON, EVENT_TRIGGERED

    @Column(name = "schedule_expression", length = 64)
    private String scheduleExpression; // Cron表达式

    @Column(name = "auto_publish")
    private Boolean autoPublish = false;

    @Column(name = "data_versioning")
    private Boolean dataVersioning = true;

    @Column(name = "status", nullable = false, length = 16)
    private String status; // DRAFT, CONFIGURED, ACTIVE, PAUSED, ARCHIVED

    @Column(name = "execution_count", nullable = false)
    private Integer executionCount = 0;

    @Column(name = "last_execution_at")
    private java.time.LocalDateTime lastExecutionAt;

    @Column(name = "last_execution_status", length = 16)
    private String lastExecutionStatus;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", length = 64)
    private String createdBy;
}