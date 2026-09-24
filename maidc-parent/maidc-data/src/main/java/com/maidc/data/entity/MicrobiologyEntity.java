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
@Table(name = "c_microbiology", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_microbiology SET is_deleted = true WHERE id = ?")
public class MicrobiologyEntity extends BaseEntity {

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "chart_date")
    private LocalDateTime chartDate;

    @Column(name = "spec_type_desc", length = 128)
    private String specTypeDesc;

    @Column(name = "test_name", length = 128)
    private String testName;

    @Column(name = "org_name", length = 256)
    private String orgName;

    @Column(name = "isolate_num")
    private Integer isolateNum;

    @Column(name = "ab_name", length = 64)
    private String abName;

    @Column(name = "dilution_text", length = 32)
    private String dilutionText;

    @Column(name = "dilution_comparison", length = 16)
    private String dilutionComparison;

    @Column(name = "dilution_value", precision = 16, scale = 6)
    private BigDecimal dilutionValue;

    @Column(name = "interpretation", length = 16)
    private String interpretation;

    @Column(name = "quantity", length = 32)
    private String quantity;
}
