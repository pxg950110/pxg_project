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
 * CDR数据质量规则实体
 * 定义数据入库前的校验规则
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "cdr_quality_rule", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.cdr_quality_rule SET is_deleted = true WHERE id = ?")
public class CdrQualityRuleEntity extends BaseEntity {

    @Column(name = "rule_code", nullable = false, unique = true, length = 64)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;

    @Column(name = "rule_category", nullable = false, length = 32)
    private String ruleCategory; // COMPLETENESS, VALIDITY, CONSISTENCY, TIMELINESS, ACCURACY

    @Column(name = "rule_type", nullable = false, length = 32)
    private String ruleType; // FIELD, RECORD, TABLE

    @Column(name = "target_schema", nullable = false, length = 32)
    private String targetSchema; // cdr, rdr

    @Column(name = "target_table", nullable = false, length = 64)
    private String targetTable;

    @Column(name = "target_column", length = 64)
    private String targetColumn;

    @Column(name = "rule_expression", columnDefinition = "jsonb")
    private String ruleExpression; // JSON格式的规则定义

    @Column(name = "severity", nullable = false, length = 16)
    private String severity; // ERROR, WARNING, INFO

    @Column(name = "failure_action", nullable = false, length = 32)
    private String failureAction; // QUARANTINE, REJECT, LOG, FIX

    @Column(name = "fix_expression", columnDefinition = "jsonb")
    private String fixExpression; // 自动修正表达式

    @Column(name = "priority", nullable = false)
    private Integer priority = 100;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "execution_count")
    private Integer executionCount = 0;

    @Column(name = "last_execution_at")
    private java.time.LocalDateTime lastExecutionAt;
}