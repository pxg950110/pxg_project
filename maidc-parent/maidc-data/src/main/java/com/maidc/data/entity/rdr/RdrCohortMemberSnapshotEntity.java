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
 * RDR队列成员快照实体
 * 存储筛选结果的患者成员列表
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "rdr_cohort_member_snapshot", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.rdr_cohort_member_snapshot SET is_deleted = true WHERE id = ?")
public class RdrCohortMemberSnapshotEntity extends BaseEntity {

    @Column(name = "result_id", nullable = false)
    private Long resultId;

    @Column(name = "cohort_id", nullable = false)
    private Long cohortId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "patient_no", nullable = false, length = 32)
    private String patientNo;

    @Column(name = "selection_order")
    private Integer selectionOrder; // 入选顺序

    @Column(name = "match_score", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal matchScore; // 匹配得分

    @Column(name = "inclusion_rule_hits", columnDefinition = "jsonb")
    private String inclusionRuleHits; // 纳入规则命中详情JSON

    @Column(name = "exclusion_rule_hits", columnDefinition = "jsonb")
    private String exclusionRuleHits; // 排除规则命中详情JSON

    @Column(name = "data_completeness", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal dataCompleteness; // 数据完整性得分

    @Column(name = "encounter_count", nullable = false)
    private Integer encounterCount; // 相关就诊次数

    @Column(name = "diagnosis_count")
    private Integer diagnosisCount; // 相关诊断数量

    @Column(name = "lab_count")
    private Integer labCount; // 相关检验数量

    @Column(name = "first_encounter_date")
    private java.time.LocalDate firstEncounterDate; // 首次相关就诊日期

    @Column(name = "last_encounter_date")
    private java.time.LocalDate lastEncounterDate; // 最后相关就诊日期

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // 是否仍在队列中

    @Column(name = "removed_at")
    private java.time.LocalDateTime removedAt; // 移出队列时间

    @Column(name = "removed_reason", length = 64)
    private String removedReason; // 移出原因

    @Column(name = "member_metadata", columnDefinition = "jsonb")
    private String memberMetadata; // 成员扩展元数据JSON
}