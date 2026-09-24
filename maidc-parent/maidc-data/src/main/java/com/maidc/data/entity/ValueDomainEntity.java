package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 值域 (Value Domain, VD)
 * 数据元允许值的集合
 *
 * 依据 WS/T 303-2023 《卫生健康信息数据元标准化规则》
 * 值域有两种子类：可枚举值域(由允许值列表规定)和不可枚举值域(由描述规定)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "value_domain", schema = "masterdata")
@SequenceGenerator(name = "id_seq", sequenceName = "value_domain_id_seq", allocationSize = 1)
public class ValueDomainEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    private Long id;

    /**
     * 值域代码
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * 值域名称
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * 英文名称
     */
    @Column(name = "name_en", length = 200)
    private String nameEn;

    /**
     * 定义
     */
    @Column(name = "definition", nullable = false, columnDefinition = "TEXT")
    private String definition;

    /**
     * 类型: ENUMERABLE(可枚举), NON_ENUMERABLE(不可枚举)
     */
    @Column(name = "domain_type", nullable = false, length = 20)
    private String domainType = "ENUMERABLE";

    /**
     * 不可枚举值域描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ==================== 数据类型 ====================

    /**
     * 数据类型: STRING, INTEGER, DECIMAL, DATE, DATETIME, BOOLEAN, CODE
     */
    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;

    /**
     * 最大长度
     */
    @Column(name = "max_length")
    private Integer maxLength;

    /**
     * 最小长度
     */
    @Column(name = "min_length")
    private Integer minLength;

    /**
     * 表示格式
     */
    @Column(name = "format", length = 100)
    private String format;

    // ==================== 计量单位 ====================

    /**
     * 计量单位代码
     */
    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;

    /**
     * 计量单位名称
     */
    @Column(name = "unit_name", length = 100)
    private String unitName;

    // ==================== 表示类 ====================

    /**
     * 表示类: AMOUNT, CODE, COUNT, DATE, TIME, TEXT, NUMBER, etc.
     */
    @Column(name = "representation_class", length = 50)
    private String representationClass;

    // ==================== 关联概念域 ====================

    /**
     * 关联的概念域ID
     */
    @Column(name = "concept_domain_id", nullable = false)
    private Long conceptDomainId;

    // ==================== 管理属性 ====================

    /**
     * 版本
     */
    @Column(name = "version", length = 20)
    private String version = "1.0";

    /**
     * 状态: DRAFT, REVIEWED, APPROVED, RETIRED
     */
    @Column(name = "status", length = 20)
    private String status = "DRAFT";

    /**
     * 注册机构
     */
    @Column(name = "registration_authority", length = 100)
    private String registrationAuthority;

    /**
     * 备注
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    // ==================== 领域方法 ====================

    /**
     * 判断是否为可枚举值域
     */
    public boolean isEnumerable() {
        return "ENUMERABLE".equals(domainType);
    }

    /**
     * 判断是否为不可枚举值域
     */
    public boolean isNonEnumerable() {
        return "NON_ENUMERABLE".equals(domainType);
    }
}
