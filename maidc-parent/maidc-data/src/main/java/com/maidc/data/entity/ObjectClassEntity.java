package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对象类 (Object Class)
 * 可以对其界限和含义进行明确的标识,且特性和行为遵循相同规则的事物的集合
 *
 * 依据 WS/T 303-2023 《卫生健康信息数据元标准化规则》
 * 对象类是概念，在面向对象的模型中与类相对应，在实体-关系模型中与实体对应
 * 例如：患者、医生、卫生机构等
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "object_class", schema = "masterdata")
@SequenceGenerator(name = "id_seq", sequenceName = "object_class_id_seq", allocationSize = 1)
public class ObjectClassEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    private Long id;

    /**
     * 对象类代码
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * 对象类名称
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
     * 父对象类ID
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
