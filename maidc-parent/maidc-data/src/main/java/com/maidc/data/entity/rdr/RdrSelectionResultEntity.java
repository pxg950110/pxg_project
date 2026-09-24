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
 * RDR队列筛选结果实体
 * 记录筛选执行的结果和统计数据
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "rdr_selection_result", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.rdr_selection_result SET is_deleted = true WHERE id = ?")
public class RdrSelectionResultEntity extends BaseEntity {

    @Column(name = "result_code", nullable = false, unique = true, length = 64)
    private String resultCode;

    @Column(name = "selection_id", nullable = false)
    private Long selectionId;

    @Column(name = "cohort_id", nullable = false)
    private Long cohortId;

    @Column(name = "execution_time", nullable = false)
    private LocalDateTime executionTime;

    @Column(name = "source_patient_count", nullable = false)
    private Long sourcePatientCount; // 原始候选患者数

    @Column(name = "passed_inclusion_count", nullable = false)
    private Long passedInclusionCount; // 通过纳入标准的患者数

    @Column(name = "passed_exclusion_count", nullable = false)
    private Long passedExclusionCount; // 通过排除标准的患者数

    @Column(name = "final_selected_count", nullable = false)
    private Long finalSelectedCount; // 最终入选患者数

    @Column(name = "excluded_count", nullable = false)
    private Long excludedCount; // 排除的患者数

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "status", nullable = false, length = 16)
    private String status; // RUNNING, COMPLETED, FAILED

    @Column(name = "rule_hit_stats", columnDefinition = "jsonb")
    private String ruleHitStats; // 规则命中统计JSON

    @Column(name = "exclusion_reason_stats", columnDefinition = "jsonb")
    private String exclusionReasonStats; // 排除原因统计JSON

    @Column(name = "data_quality_stats", columnDefinition = "jsonb")
    private String dataQualityStats; // 数据质量统计JSON

    @Column(name = "snapshot_table_name", length = 64)
    private String snapshotTableName; // 成员快照表名

    @Column(name = "is_snapshot_created")
    private Boolean isSnapshotCreated = false;

    @Column(name = "triggered_by", length = 64)
    private String triggeredBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}