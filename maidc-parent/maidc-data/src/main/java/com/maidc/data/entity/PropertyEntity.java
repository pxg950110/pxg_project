package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 特性 (Property)
 * 一个对象类所有成员所共有的特征
 *
 * 依据 WS/T 303-2023 《卫生健康信息数据元标准化规则》
 * 特性用来区别和描述对象，是对象类的特征
 * 例如：身高、体重、血压、脉搏、血型等
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "property", schema = "masterdata")
@SequenceGenerator(name = "id_seq", sequenceName = "property_id_seq", allocationSize = 1)
public class PropertyEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    private Long id;

    /**
     * 特性代码
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * 特性名称
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
     * 概念类型: GENERAL(一般概念), INDIVIDUAL(个别概念)
     */
    @Column(name = "concept_type", length = 20)
    private String conceptType = "GENERAL";

    /**
     * 父特性ID
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
     * 备注
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;
}
