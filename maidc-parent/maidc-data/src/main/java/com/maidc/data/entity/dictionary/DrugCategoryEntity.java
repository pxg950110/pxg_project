package com.maidc.data.entity.dictionary;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

/**
 * 药品分类实体（树形结构）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@DynamicUpdate
@Table(name = "d_drug_category", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SequenceGenerator(name = "id_seq", sequenceName = "d_drug_category_id_seq", allocationSize = 1)
public class DrugCategoryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    private Long id;

    /** 分类编码 */
    @Column(name = "code", nullable = false, length = 64, unique = true)
    private String code;

    /** 分类名称 */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** 英文名称 */
    @Column(name = "name_en", length = 128)
    private String nameEn;

    /** 父分类ID */
    @Column(name = "parent_id")
    private Long parentId;

    /** 层级 */
    @Column(name = "level", nullable = false)
    private Integer level = 1;

    /** 排序号 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /** 状态：ACTIVE/INACTIVE */
    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    /** 备注 */
    @Column(name = "remark", columnDefinition = "text")
    private String remark;

    /** 子分类（仅用于树形响应，不落库） */
    @Transient
    private List<DrugCategoryEntity> children = new ArrayList<>();
}