package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 值含义 (Value Meaning, VM)
 * 一个值的含义或语义内容
 *
 * 依据 WS/T 303-2023 《卫生健康信息数据元标准化规则》
 * 值含义是概念域的内涵，可枚举概念域的值含义可以明确地列举
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "value_meaning", schema = "masterdata",
        uniqueConstraints = @UniqueConstraint(columnNames = {"concept_domain_id", "code"}))
@SequenceGenerator(name = "id_seq", sequenceName = "value_meaning_id_seq", allocationSize = 1)
public class ValueMeaningEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    private Long id;

    /**
     * 所属概念域ID
     */
    @Column(name = "concept_domain_id", nullable = false)
    private Long conceptDomainId;

    /**
     * 值含义代码
     */
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    /**
     * 值含义名称
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
    @Column(name = "definition", columnDefinition = "TEXT")
    private String definition;

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
}