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

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "m_reference_range", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_reference_range SET is_deleted = true WHERE id = ?")
public class ReferenceRangeEntity extends BaseEntity {

    @Column(name = "concept_id", nullable = false)
    private Long conceptId;

    @Column(name = "gender", nullable = false, length = 8)
    private String gender = "ALL";

    @Column(name = "age_min", precision = 10, scale = 2)
    private BigDecimal ageMin;

    @Column(name = "age_max", precision = 10, scale = 2)
    private BigDecimal ageMax;

    @Column(name = "range_low", precision = 10, scale = 2)
    private BigDecimal rangeLow;

    @Column(name = "range_high", precision = 10, scale = 2)
    private BigDecimal rangeHigh;

    @Column(name = "unit", length = 32)
    private String unit;

    @Column(name = "critical_low", precision = 10, scale = 2)
    private BigDecimal criticalLow;

    @Column(name = "critical_high", precision = 10, scale = 2)
    private BigDecimal criticalHigh;

    @Column(name = "source", length = 128)
    private String source;
}
