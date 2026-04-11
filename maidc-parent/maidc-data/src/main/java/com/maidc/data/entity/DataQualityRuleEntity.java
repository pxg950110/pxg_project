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
@Table(name = "r_data_quality_rule", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_data_quality_rule SET is_deleted = true WHERE id = ?")
public class DataQualityRuleEntity extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;

    @Column(name = "rule_type", nullable = false, length = 32)
    private String ruleType;

    @Column(name = "target_table", length = 64)
    private String targetTable;

    @Column(name = "target_column", length = 64)
    private String targetColumn;

    @Column(name = "rule_expr", columnDefinition = "jsonb")
    private String ruleExpr;

    @Column(name = "threshold", nullable = false, precision = 5, scale = 2)
    private BigDecimal threshold = new BigDecimal("100.00");

    @Column(name = "severity", nullable = false, length = 16)
    private String severity;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
}
