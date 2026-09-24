package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 概念域 (Conceptual Domain, CD)
 * 有效的值含义的集合
 *
 * 依据 WS/T 303-2023 《卫生健康信息数据元标准化规则》
 * 概念的外延构成了概念域，一个概念域是一个值含义集合
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "concept_domain", schema = "masterdata")
@SequenceGenerator(name = "id_seq", sequenceName = "concept_domain_id_seq", allocationSize = 1)
public class ConceptDomainEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    private Long id;

    /**
     * 概念域代码
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * 概念域名称
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
     * 不可枚举概念域描述规则
     */
    @Column(name = "description_rule", columnDefinition = "TEXT")
    private String descriptionRule;

    /**
     * 维度(等价计量单位的共同特征)
     * 例如: 长度、重量、温度等
     */
    @Column(name = "dimension", length = 100)
    private String dimension;

    /**
     * 父概念域ID(用于概念体系)
     */
    @Column(name = "parent_id")
    private Long parentId;

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
     * 判断是否为可枚举概念域
     */
    public boolean isEnumerable() {
        return "ENUMERABLE".equals(domainType);
    }

    /**
     * 判断是否为不可枚举概念域
     */
    public boolean isNonEnumerable() {
        return "NON_ENUMERABLE".equals(domainType);
    }
}
