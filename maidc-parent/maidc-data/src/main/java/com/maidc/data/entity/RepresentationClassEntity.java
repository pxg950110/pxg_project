package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表示类 (Representation Class)
 * WS/T 303-2023 值域"表示类"分类字典（金额/代码/计数/日期/时间/文本/数值等）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "representation_class", schema = "masterdata")
public class RepresentationClassEntity extends BaseEntity {

    /**
     * 表示类代码: AMOUNT, CODE, COUNT, DATE, DATETIME, TIME, TEXT, NUMBER
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * 表示类名称
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
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /**
     * 是否有效
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
