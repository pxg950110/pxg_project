package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "m_local_concept", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_local_concept SET is_deleted = true WHERE id = ?")
public class LocalConceptEntity extends BaseEntity {

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(name = "code_system_id", nullable = false)
    private Long codeSystemId;

    @Column(name = "local_code", nullable = false, length = 64)
    private String localCode;

    @Column(name = "local_name", nullable = false, length = 512)
    private String localName;

    @Column(name = "standard_concept_id")
    private Long standardConceptId;

    @Column(name = "mapping_confidence", precision = 3, scale = 2)
    private BigDecimal mappingConfidence;

    @Column(name = "mapping_status", nullable = false, length = 16)
    private String mappingStatus = "UNMAPPED";

    @Column(name = "mapped_by", length = 64)
    private String mappedBy;

    @Column(name = "mapped_at")
    private LocalDateTime mappedAt;
}
