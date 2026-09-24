package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据元概念 (Data Element Concept, DEC)
 * 能以数据元的形式表示的概念，其表述与任何特定表示法无关
 *
 * 依据 WS/T 303-2023 《卫生健康信息数据元标准化规则》
 * 一个数据元概念是由对象类和特性两部分组成
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "data_element_concept", schema = "masterdata")
@SequenceGenerator(name = "id_seq", sequenceName = "data_element_concept_id_seq", allocationSize = 1)
public class DataElementConceptEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    private Long id;

    /**
     * 数据元概念代码
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * 数据元概念名称
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

    // ==================== 对象类 (Object Class) ====================

    /**
     * 对象类代码
     */
    @Column(name = "object_class_code", length = 50)
    private String objectClassCode;

    /**
     * 对象类名称
     */
    @Column(name = "object_class_name", length = 200)
    private String objectClassName;

    /**
     * 对象类定义
     */
    @Column(name = "object_class_definition", columnDefinition = "TEXT")
    private String objectClassDefinition;

    // ==================== 特性 (Property) ====================

    /**
     * 特性代码
     */
    @Column(name = "property_code", length = 50)
    private String propertyCode;

    /**
     * 特性名称
     */
    @Column(name = "property_name", length = 200)
    private String propertyName;

    /**
     * 特性定义
     */
    @Column(name = "property_definition", columnDefinition = "TEXT")
    private String propertyDefinition;

    // ==================== 关联概念域 ====================

    /**
     * 关联的概念域ID
     */
    @Column(name = "concept_domain_id")
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
}
