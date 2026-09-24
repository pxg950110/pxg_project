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
 * CDR数据隔离区实体
 * 存储未通过质量校验的数据
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "cdr_quarantine_data", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.cdr_quarantine_data SET is_deleted = true WHERE id = ?")
public class CdrQuarantineDataEntity extends BaseEntity {

    @Column(name = "quarantine_code", nullable = false, unique = true, length = 64)
    private String quarantineCode;

    @Column(name = "source_schema", nullable = false, length = 32)
    private String sourceSchema;

    @Column(name = "source_table", nullable = false, length = 64)
    private String sourceTable;

    @Column(name = "source_batch_id")
    private Long sourceBatchId;

    @Column(name = "original_data", columnDefinition = "jsonb")
    private String originalData; // 原始数据JSON

    @Column(name = "record_id", length = 64)
    private String recordId;

    @Column(name = "failed_rules", columnDefinition = "jsonb")
    private String failedRules; // 失败规则列表JSON

    @Column(name = "error_count", nullable = false)
    private Integer errorCount;

    @Column(name = "quarantine_time", nullable = false)
    private LocalDateTime quarantineTime;

    @Column(name = "priority", nullable = false, length = 16)
    private String priority; // HIGH, MEDIUM, LOW

    @Column(name = "status", nullable = false, length = 32)
    private String status; // PENDING, ASSIGNED, PROCESSING, FIXED, DISCARDED, RELEASED

    @Column(name = "assigned_to", length = 64)
    private String assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "processed_by", length = 64)
    private String processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "process_action", length = 32)
    private String processAction; // FIX, DISCARD, RELEASE

    @Column(name = "fixed_data", columnDefinition = "jsonb")
    private String fixedData;

    @Column(name = "process_notes", columnDefinition = "TEXT")
    private String processNotes;

    @Column(name = "release_target")
    private Long releaseTarget; // 释放后写入的目标表ID
}