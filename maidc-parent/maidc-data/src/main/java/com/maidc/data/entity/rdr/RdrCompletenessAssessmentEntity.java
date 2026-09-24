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
 * RDR数据完整性评估实体
 * 记录患者数据的多模态完整性评估结果
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "rdr_completeness_assessment", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.rdr_completeness_assessment SET is_deleted = true WHERE id = ?")
public class RdrCompletenessAssessmentEntity extends BaseEntity {

    @Column(name = "assessment_code", nullable = false, unique = true, length = 64)
    private String assessmentCode;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "cohort_id")
    private Long cohortId;

    @Column(name = "assessment_time", nullable = false)
    private LocalDateTime assessmentTime;

    @Column(name = "overall_score", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal overallScore; // 整体完整性得分

    @Column(name = "clinical_score", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal clinicalScore; // 临床数据得分

    @Column(name = "imaging_score", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal imagingScore; // 影像数据得分

    @Column(name = "genomic_score", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal genomicScore; // 基因组数据得分

    @Column(name = "text_score", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal textScore; // 文本数据得分

    @Column(name = "waveform_score", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal waveformScore; // 波形数据得分

    @Column(name = "pathology_score", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal pathologyScore; // 病理数据得分

    @Column(name = "missing_modality_list", columnDefinition = "jsonb")
    private String missingModalityList; // 缺失模态列表JSON

    @Column(name = "gap_analysis", columnDefinition = "jsonb")
    private String gapAnalysis; // 数据缺口分析JSON

    @Column(name = "recommendations", columnDefinition = "jsonb")
    private String recommendations; // 改进建议JSON

    @Column(name = "completeness_level", length = 16)
    private String completenessLevel; // HIGH, MEDIUM, LOW, CRITICAL

    @Column(name = "data_elements_expected", nullable = false)
    private Integer dataElementsExpected; // 期望数据元数量

    @Column(name = "data_elements_present", nullable = false)
    private Integer dataElementsPresent; // 实际存在数据元数量

    @Column(name = "critical_elements_missing")
    private Integer criticalElementsMissing; // 关键数据元缺失数

    @Column(name = "time_range_start")
    private java.time.LocalDate timeRangeStart;

    @Column(name = "time_range_end")
    private java.time.LocalDate timeRangeEnd;

    @Column(name = "encounter_count")
    private Integer encounterCount; // 就诊次数

    @Column(name = "status", nullable = false, length = 16)
    private String status; // COMPLETED, PARTIAL, FAILED

    @Column(name = "assessed_by", length = 64)
    private String assessedBy; // MANUAL, AUTO_SYSTEM

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}