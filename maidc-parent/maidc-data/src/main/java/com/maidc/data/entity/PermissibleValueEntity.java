package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

/**
 * 允许值/值集 (Permissible Value, PV)
 * 在一个特定值域中允许的一个值含义的表达
 *
 * 依据 WS/T 303-2023 《卫生健康信息数据元标准化规则》
 * 一个允许值是某个值和该值的含义的组合
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "permissible_value", schema = "masterdata",
        uniqueConstraints = @UniqueConstraint(columnNames = {"value_domain_id", "value"}))
@SequenceGenerator(name = "id_seq", sequenceName = "permissible_value_id_seq", allocationSize = 1)
public class PermissibleValueEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    private Long id;

    /**
     * 所属值域ID
     */
    @Column(name = "value_domain_id", nullable = false)
    private Long valueDomainId;

    /**
     * 值
     */
    @Column(name = "value", nullable = false, length = 200)
    private String value;

    /**
     * 关联值含义ID
     */
    @Column(name = "value_meaning_id")
    private Long valueMeaningId;

    /**
     * 值含义名称(冗余存储)
     */
    @Column(name = "value_meaning_name", length = 200)
    private String valueMeaningName;

    /**
     * 排序号
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * 是否有效
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * 生效日期
     */
    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    /**
     * 失效日期
     */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // ==================== 领域方法 ====================

    /**
     * 判断是否在有效期内
     */
    public boolean isEffective(LocalDate date) {
        if (effectiveDate != null && date.isBefore(effectiveDate)) {
            return false;
        }
        if (expiryDate != null && date.isAfter(expiryDate)) {
            return false;
        }
        return isActive;
    }

    /**
     * 判断当前是否有效
     */
    public boolean isCurrentlyEffective() {
        return isEffective(LocalDate.now());
    }
}