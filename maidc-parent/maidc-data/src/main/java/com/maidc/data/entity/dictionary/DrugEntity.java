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
 * 药品字典实体
 * 支持多编码体系映射（国家编码、ATC编码、医保编码、院内编码等）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@DynamicUpdate
@Table(name = "d_drug", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SequenceGenerator(name = "id_seq", sequenceName = "d_drug_id_seq", allocationSize = 1)
public class DrugEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    private Long id;

    /** 药品编码（主键编码） */
    @Column(name = "drug_code", nullable = false, length = 64, unique = true)
    private String drugCode;

    /** 药品名称（通用名） */
    @Column(name = "name", nullable = false, length = 256)
    private String name;

    /** 商品名 */
    @Column(name = "trade_name", length = 256)
    private String tradeName;

    /** 英文名称 */
    @Column(name = "name_en", length = 256)
    private String nameEn;

    /** 拼音码（用于快速检索） */
    @Column(name = "name_pinyin", length = 512)
    private String namePinyin;

    // ==================== 分类信息 ====================

    /** 药理分类ID */
    @Column(name = "category_id")
    private Long categoryId;

    /** 药理分类名称 */
    @Column(name = "category_name", length = 128)
    private String categoryName;

    /** ATC编码（国际药品分类编码） */
    @Column(name = "atc_code", length = 16)
    private String atcCode;

    // ==================== 规格信息 ====================

    /** 剂型 */
    @Column(name = "dosage_form", length = 64)
    private String dosageForm;

    /** 规格 */
    @Column(name = "specification", length = 128)
    private String specification;

    /** 计量单位 */
    @Column(name = "unit", length = 32)
    private String unit;

    /** 包装单位 */
    @Column(name = "pack_unit", length = 32)
    private String packUnit;

    /** 包装数量 */
    @Column(name = "pack_quantity", precision = 10, scale = 2)
    private BigDecimal packQuantity;

    // ==================== 价格信息 ====================

    /** 单价 */
    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    /** 价格单位 */
    @Column(name = "price_unit", length = 32)
    private String priceUnit;

    // ==================== 医保信息 ====================

    /** 医保编码 */
    @Column(name = "insurance_code", length = 64)
    private String insuranceCode;

    /** 医保类别（甲类/乙类/丙类/自费） */
    @Column(name = "insurance_type", length = 16)
    private String insuranceType;

    /** 医保报销比例 */
    @Column(name = "insurance_ratio", precision = 5, scale = 4)
    private BigDecimal insuranceRatio;

    // ==================== 厂家信息 ====================

    /** 生产厂家 */
    @Column(name = "manufacturer", length = 256)
    private String manufacturer;

    /** 厂家编码 */
    @Column(name = "manufacturer_code", length = 64)
    private String manufacturerCode;

    // ==================== 多编码映射 ====================

    /** 国家药品编码 */
    @Column(name = "national_code", length = 64)
    private String nationalCode;

    /** 院内编码 */
    @Column(name = "hospital_code", length = 64)
    private String hospitalCode;

    /** HIS系统编码 */
    @Column(name = "his_code", length = 64)
    private String hisCode;

    // ==================== 状态与扩展 ====================

    /** 状态：ACTIVE/INACTIVE/RETIRED */
    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    /** 是否OTC药品 */
    @Column(name = "is_otc")
    private Boolean isOtc = false;

    /** 是否处方药 */
    @Column(name = "is_prescription")
    private Boolean isPrescription = true;

    /** 管制级别：NORMAL/精神/麻醉 */
    @Column(name = "is_controlled", length = 16)
    private String isControlled = "NORMAL";

    /** 扩展属性（JSONB） */
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