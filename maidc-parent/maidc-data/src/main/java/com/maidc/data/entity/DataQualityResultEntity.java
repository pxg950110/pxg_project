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
@Table(name = "r_data_quality_result", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_data_quality_result SET is_deleted = true WHERE id = ?")
public class DataQualityResultEntity extends BaseEntity {

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "total_records")
    private Long totalRecords;

    @Column(name = "passed_records")
    private Long passedRecords;

    @Column(name = "failed_records")
    private Long failedRecords;

    @Column(name = "pass_rate", precision = 5, scale = 2)
    private BigDecimal passRate;

    @Column(name = "detail", columnDefinition = "jsonb")
    private String detail;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;
}
