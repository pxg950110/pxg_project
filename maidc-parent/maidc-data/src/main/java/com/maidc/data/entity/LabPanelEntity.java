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

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "c_lab_panel", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_lab_panel SET is_deleted = true WHERE id = ?")
public class LabPanelEntity extends BaseEntity {

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "item_code", length = 32)
    private String itemCode;

    @Column(name = "item_name", length = 64)
    private String itemName;

    @Column(name = "result_value", columnDefinition = "TEXT")
    private String resultValue;

    @Column(name = "result_unit", length = 32)
    private String resultUnit;

    @Column(name = "reference_range", length = 64)
    private String referenceRange;

    @Column(name = "abnormal_flag", nullable = false)
    private Boolean abnormalFlag = false;
}
