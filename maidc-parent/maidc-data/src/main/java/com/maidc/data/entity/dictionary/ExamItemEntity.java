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
 * 检查项目字典实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@DynamicUpdate
@Table(name = "d_exam_item", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SequenceGenerator(name = "id_seq", sequenceName = "d_exam_item_id_seq", allocationSize = 1)
public class ExamItemEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    private Long id;

    /** 检查编码 */
    @Column(name = "exam_code", nullable = false, length = 64, unique = true)
    private String examCode;

    /** 项目名称 */
    @Column(name = "name", nullable = false, length = 256)
    private String name;

    /** 英文名称 */
    @Column(name = "name_en", length = 256)
    private String nameEn;

    /** 拼音码 */
    @Column(name = "name_pinyin", length = 512)
    private String namePinyin;

    // ==================== 检查信息 ====================

    /** 检查类型（CT/MRI/X线/超声/心电/内镜） */
    @Column(name = "exam_type", nullable = false, length = 32)
    private String examType;

    /** 检查部位 */
    @Column(name = "body_site", length = 128)
    private String bodySite;

    /** 部位编码 */
    @Column(name = "body_site_code", length = 32)
    private String bodySiteCode;

    /** 检查方法 */
    @Column(name = "method", length = 128)
    private String method;

    /** 造影类型（无/增强/造影） */
    @Column(name = "contrast_type", length = 32)
    private String contrastType;

    // ==================== 设备信息 ====================

    /** 设备类型 */
    @Column(name = "equipment_type", length = 64)
    private String equipmentType;

    /** 模态 */
    @Column(name = "modality", length = 32)
    private String modality;

    // ==================== 关联信息 ====================

    /** 检查分类 */
    @Column(name = "exam_category", length = 64)
    private String examCategory;

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