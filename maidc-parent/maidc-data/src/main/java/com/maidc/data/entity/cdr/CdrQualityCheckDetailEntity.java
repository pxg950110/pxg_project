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
 * CDR数据质量检测明细实体
 * 记录每条记录的检测结果
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "cdr_quality_check_detail", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.cdr_quality_check_detail SET is_deleted = true WHERE id = ?")
public class CdrQualityCheckDetailEntity extends BaseEntity {

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "record_id", length = 64)
    private String recordId; // 被检测记录的主键值

    @Column(name = "record_identifier", length = 256)
    private String recordIdentifier; // 记录标识符，如患者编号

    @Column(name = "target_column", length = 64)
    private String targetColumn;

    @Column(name = "actual_value", columnDefinition = "TEXT")
    private String actualValue;

    @Column(name = "expected_value", columnDefinition = "TEXT")
    private String expectedValue;

    @Column(name = "error_type", nullable = false, length = 32)
    private String errorType; // MISSING, INVALID, OUT_OF_RANGE, FORMAT, DUPLICATE, INCONSISTENT

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "severity", nullable = false, length = 16)
    private String severity; // ERROR, WARNING, INFO

    @Column(name = "check_result", nullable = false, length = 16)
    private String checkResult; // PASS, FAIL

    @Column(name = "is_quarantined")
    private Boolean isQuarantined = false;

    @Column(name = "quarantine_id")
    private Long quarantineId;

    @Column(name = "is_fixed")
    private Boolean isFixed = false;

    @Column(name = "fixed_value", columnDefinition = "TEXT")
    private String fixedValue;
}