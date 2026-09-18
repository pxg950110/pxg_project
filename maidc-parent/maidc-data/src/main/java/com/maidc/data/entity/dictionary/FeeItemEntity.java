package com.maidc.data.entity.dictionary;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收费项目字典实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@DynamicUpdate
@Table(name = "d_fee_item", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SequenceGenerator(name = "id_seq", sequenceName = "d_fee_item_id_seq", allocationSize = 1)
public class FeeItemEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    private Long id;

    /** 项目编码 */
    @Column(name = "fee_code", nullable = false, length = 64, unique = true)
    private String feeCode;

    /** 项目名称 */
    @Column(name = "name", nullable = false, length = 256)
    private String name;

    /** 英文名称 */
    @Column(name = "name_en", length = 256)
    private String nameEn;

    /** 拼音码 */
    @Column(name = "name_pinyin", length = 512)
    private String namePinyin;

    // ==================== 分类信息 ====================

    /** 收费类别（药品/诊疗/检验/检查/材料/服务） */
    @Column(name = "fee_category", nullable = false, length = 32)
    private String feeCategory;

    /** 费别（甲类/乙类/丙类/自费） */
    @Column(name = "fee_type", length = 32)
    private String feeType;

    // ==================== 价格信息 ====================

    /** 价格 */
    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    /** 计价单位 */
    @Column(name = "unit", length = 32)
    private String unit;

    /** 执行单位 */
    @Column(name = "execution_unit", length = 32)
    private String executionUnit;

    // ==================== 医保信息 ====================

    /** 医保编码 */
    @Column(name = "insurance_code", length = 64)
    private String insuranceCode;

    /** 医保类别 */
    @Column(name = "insurance_type", length = 16)
    private String insuranceType;

    /** 报销比例 */
    @Column(name = "insurance_ratio", precision = 5, scale = 4)
    private BigDecimal insuranceRatio;

    // ==================== 科室关联 ====================

    /** 执行科室编码 */
    @Column(name = "executing_dept", length = 64)
    private String executingDept;

    /** 执行科室名称 */
    @Column(name = "executing_dept_name", length = 128)
    private String executingDeptName;

    // ==================== 关联编码 ====================

    /** 关联概念ID */
    @Column(name = "related_concept_id")
    private Long relatedConceptId;

    // ==================== 状态与扩展 ====================

    /** 状态 */
    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    /** 是否可批量计费 */
    @Column(name = "is_batch")
    private Boolean isBatch = false;

    /** 扩展属性 */
    @Column(name = "properties", columnDefinition = "jsonb")
    private String properties;

    /** 生效日期 */
    @Column(name = "valid_from")
    private LocalDate validFrom;

    /** 失效日期 */
    @Column(name = "valid_to")
    private LocalDate validTo;

    /** 备注 */
    @Column(name = "remark", columnDefinition = "text")
    private String remark;
}