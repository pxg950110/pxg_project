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
 * RDR多模态关联组实体
 * 将相关的一组多模态数据组织在一起
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "rdr_multimodal_link_group", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.rdr_multimodal_link_group SET is_deleted = true WHERE id = ?")
public class RdrMultimodalLinkGroupEntity extends BaseEntity {

    @Column(name = "group_code", nullable = false, unique = true, length = 64)
    private String groupCode;

    @Column(name = "group_name", nullable = false, length = 128)
    private String groupName;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "group_type", nullable = false, length = 32)
    private String groupType; // ENCOUNTER_BASED, TIME_WINDOW, STUDY_SPECIFIC

    @Column(name = "time_window_start")
    private java.time.LocalDateTime timeWindowStart;

    @Column(name = "time_window_end")
    private java.time.LocalDateTime timeWindowEnd;

    @Column(name = "clinical_count", nullable = false)
    private Integer clinicalCount = 0;

    @Column(name = "imaging_count", nullable = false)
    private Integer imagingCount = 0;

    @Column(name = "genomic_count", nullable = false)
    private Integer genomicCount = 0;

    @Column(name = "text_count", nullable = false)
    private Integer textCount = 0;

    @Column(name = "waveform_count", nullable = false)
    private Integer waveformCount = 0;

    @Column(name = "pathology_count", nullable = false)
    private Integer pathologyCount = 0;

    @Column(name = "total_link_count", nullable = false)
    private Integer totalLinkCount = 0;

    @Column(name = "completeness_score", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal completenessScore; // 完整性得分

    @Column(name = "modality_coverage", columnDefinition = "jsonb")
    private String modalityCoverage; // 模态覆盖情况JSON

    @Column(name = "primary_diagnosis", length = 128)
    private String primaryDiagnosis; // 主要诊断

    @Column(name = "study_context", columnDefinition = "jsonb")
    private String studyContext; // 研究上下文JSON

    @Column(name = "status", nullable = false, length = 16)
    private String status; // ACTIVE, CLOSED, ARCHIVED

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "group_metadata", columnDefinition = "jsonb")
    private String groupMetadata; // 扩展元数据JSON
}