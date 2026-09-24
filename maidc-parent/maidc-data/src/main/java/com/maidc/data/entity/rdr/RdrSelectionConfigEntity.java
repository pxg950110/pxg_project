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
 * RDR队列筛选配置实体
 * 定义科研队列的患者筛选规则
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "rdr_selection_config", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.rdr_selection_config SET is_deleted = true WHERE id = ?")
public class RdrSelectionConfigEntity extends BaseEntity {

    @Column(name = "selection_code", nullable = false, unique = true, length = 64)
    private String selectionCode;

    @Column(name = "selection_name", nullable = false, length = 128)
    private String selectionName;

    @Column(name = "cohort_id", nullable = false)
    private Long cohortId;

    @Column(name = "data_source", nullable = false, length = 32)
    private String dataSource; // CDR, RDR

    @Column(name = "time_range_start")
    private java.time.LocalDate timeRangeStart;

    @Column(name = "time_range_end")
    private java.time.LocalDate timeRangeEnd;

    @Column(name = "encounter_type", length = 32)
    private String encounterType; // INPATIENT, OUTPATIENT, ALL

    @Column(name = "inclusion_rules", columnDefinition = "jsonb")
    private String inclusionRules; // 纳入规则JSON数组

    @Column(name = "exclusion_rules", columnDefinition = "jsonb")
    private String exclusionRules; // 排除规则JSON数组

    @Column(name = "rule_logic", length = 32)
    private String ruleLogic; // AND, OR, CUSTOM

    @Column(name = "min_encounters")
    private Integer minEncounters; // 最少就诊次数

    @Column(name = "min_data_age")
    private Integer minDataAge; // 数据最小年龄(天)

    @Column(name = "data_completeness_threshold", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal dataCompletenessThreshold; // 数据完整性阈值

    @Column(name = "template_id")
    private Long templateId; // 关联的规则模板ID

    @Column(name = "status", nullable = false, length = 16)
    private String status; // DRAFT, ACTIVE, PAUSED, ARCHIVED

    @Column(name = "execution_count", nullable = false)
    private Integer executionCount = 0;

    @Column(name = "last_execution_at")
    private LocalDateTime lastExecutionAt;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", length = 64)
    private String createdBy;
}