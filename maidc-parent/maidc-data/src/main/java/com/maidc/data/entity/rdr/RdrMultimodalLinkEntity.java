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
 * RDR多模态数据关联实体
 * 管理不同模态数据之间的关联关系
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "rdr_multimodal_link", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.rdr_multimodal_link SET is_deleted = true WHERE id = ?")
public class RdrMultimodalLinkEntity extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "link_group_id")
    private Long linkGroupId; // 关联组ID

    @Column(name = "modality_type", nullable = false, length = 32)
    private String modalityType; // CLINICAL, IMAGING, GENOMIC, TEXT, WAVEFORM, PATHOLOGY

    @Column(name = "data_category", nullable = false, length = 64)
    private String dataCategory; // 数据类别：诊断、检验、影像等

    @Column(name = "source_table", nullable = false, length = 64)
    private String sourceTable; // 来源表名

    @Column(name = "source_record_id", nullable = false, length = 64)
    private String sourceRecordId; // 来源记录ID

    @Column(name = "external_reference", length = 256)
    private String externalReference; // 外部引用路径/URL

    @Column(name = "timestamp")
    private java.time.LocalDateTime timestamp; // 数据时间戳

    @Column(name = "link_strength", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal linkStrength; // 关联强度得分

    @Column(name = "link_type", nullable = false, length = 32)
    private String linkType; // TEMPORAL, SEMANTIC, CAUSAL, SPATIAL

    @Column(name = "link_confidence", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal linkConfidence; // 关联置信度

    @Column(name = "link_source", length = 32)
    private String linkSource; // MANUAL, AUTO_TEMPORAL, AUTO_SEMANTIC, AUTO_AI

    @Column(name = "link_metadata", columnDefinition = "jsonb")
    private String linkMetadata; // 关联元数据JSON

    @Column(name = "is_validated")
    private Boolean isValidated = false;

    @Column(name = "validated_by", length = 64)
    private String validatedBy;

    @Column(name = "validated_at")
    private java.time.LocalDateTime validatedAt;

    @Column(name = "status", nullable = false, length = 16)
    private String status; // ACTIVE, INACTIVE, DEPRECATED

    @Column(name = "quality_score", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal qualityScore; // 数据质量得分

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}