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

/**
 * CDR术语映射实体
 * 本地术语到标准术语(LOINC/ICD10/SNOMED/ATC)的映射
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "cdr_term_mapping", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.cdr_term_mapping SET is_deleted = true WHERE id = ?")
public class CdrTermMappingEntity extends BaseEntity {

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Column(name = "term_type", nullable = false, length = 32)
    private String termType; // LAB, DIAG, DRUG, PROC, OBS

    @Column(name = "local_code", nullable = false, length = 64)
    private String localCode;

    @Column(name = "local_name", nullable = false, length = 256)
    private String localName;

    @Column(name = "standard_system", nullable = false, length = 32)
    private String standardSystem; // LOINC, ICD10, SNOMED_CT, ATC, CPT

    @Column(name = "standard_code", nullable = false, length = 64)
    private String standardCode;

    @Column(name = "standard_name", nullable = false, length = 256)
    private String standardName;

    @Column(name = "mapping_type", nullable = false, length = 32)
    private String mappingType; // EXACT, NARROWER, BROADER, APPROXIMATE, MANUAL

    @Column(name = "confidence", columnDefinition = "DECIMAL(5,2)")
    private java.math.BigDecimal confidence;

    @Column(name = "mapping_status", nullable = false, length = 16)
    private String mappingStatus; // PENDING, VALIDATED, CONFIRMED, REJECTED

    @Column(name = "validated_by", length = 64)
    private String validatedBy;

    @Column(name = "validated_at")
    private java.time.LocalDateTime validatedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_preferred")
    private Boolean isPreferred = false;

    @Column(name = "use_count", nullable = false)
    private Integer useCount = 0;

    @Column(name = "last_used_at")
    private java.time.LocalDateTime lastUsedAt;
}