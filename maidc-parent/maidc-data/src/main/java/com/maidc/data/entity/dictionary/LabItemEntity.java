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
 * 检验项目字典实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@DynamicUpdate
@Table(name = "d_lab_item", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SequenceGenerator(name = "id_seq", sequenceName = "d_lab_item_id_seq", allocationSize = 1)
public class LabItemEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    private Long id;

    /** 检验编码 */
    @Column(name = "lab_code", nullable = false, length = 64, unique = true)
    private String labCode;

    /** 项目名称 */
    @Column(name = "name", nullable = false, length = 256)
    private String name;

    /** 英文名称 */
    @Column(name = "name_en", length = 256)
    private String nameEn;

    /** 拼音码 */
    @Column(name = "name_pinyin", length = 512)
    private String namePinyin;

    // ==================== LOINC编码映射 ====================

    /** LOINC编码 */
    @Column(name = "loinc_code", length = 16)
    private String loincCode;

    /** LOINC标准名称 */
    @Column(name = "loinc_name", length = 512)
    private String loincName;

    // ==================== 检验信息 ====================

    /** 标本类型 */
    @Column(name = "specimen_type", length = 64)
    private String specimenType;

    /** 标本类型编码 */
    @Column(name = "specimen_type_code", length = 32)
    private String specimenTypeCode;

    /** 检验方法 */
    @Column(name = "method", length = 128)
    private String method;

    /** 是否组合项目 */
    @Column(name = "is_panel")
    private Boolean isPanel = false;

    // ==================== 结果信息 ====================

    /** 结果类型（数值/文本/定性） */
    @Column(name = "result_type", length = 32)
    private String resultType;

    /** 结果单位 */
    @Column(name = "unit", length = 32)
    private String unit;

    // ==================== 参考范围（通用） ====================

    /** 参考范围下限 */
    @Column(name = "ref_range_low", precision = 12, scale = 4)
    private BigDecimal refRangeLow;

    /** 参考范围上限 */
    @Column(name = "ref_range_high", precision = 12, scale = 4)
    private BigDecimal refRangeHigh;

    /** 参考范围文本描述 */
    @Column(name = "ref_range_text", length = 256)
    private String refRangeText;

    /** 危急值下限 */
    @Column(name = "critical_low", precision = 12, scale = 4)
    private BigDecimal criticalLow;

    /** 危急值上限 */
    @Column(name = "critical_high", precision = 12, scale = 4)
    private BigDecimal criticalHigh;

    // ==================== 关联信息 ====================

    /** 检验分类 */
    @Column(name = "lab_category", length = 64)
    private String labCategory;

    /** 父项目ID（组合项目） */
    @Column(name = "parent_id")
    private Long parentId;

    /** 关联收费项目 */
    @Column(name = "fee_item_id")
    private Long feeItemId;

    // ==================== 状态与扩展 ====================

    /** 状态 */
    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

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