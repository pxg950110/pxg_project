package com.maidc.data.entity.dictionary;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

/**
 * 诊断字典实体
 * 支持 ICD-10 层级结构
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity(name = "DiagnosisDict")
@DynamicUpdate
@Table(name = "d_diagnosis", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SequenceGenerator(name = "id_seq", sequenceName = "d_diagnosis_id_seq", allocationSize = 1)
public class DiagnosisEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    private Long id;

    /** 诊断编码 */
    @Column(name = "diagnosis_code", nullable = false, length = 32, unique = true)
    private String diagnosisCode;

    /** 诊断名称 */
    @Column(name = "name", nullable = false, length = 512)
    private String name;

    /** 英文名称 */
    @Column(name = "name_en", length = 512)
    private String nameEn;

    /** 拼音码 */
    @Column(name = "name_pinyin", length = 1024)
    private String namePinyin;

    // ==================== ICD编码映射 ====================

    /** ICD-10编码 */
    @Column(name = "icd10_code", length = 16)
    private String icd10Code;

    /** ICD-10标准名称 */
    @Column(name = "icd10_name", length = 512)
    private String icd10Name;

    /** ICD-9-CM编码 */
    @Column(name = "icd9cm_code", length = 16)
    private String icd9cmCode;

    // ==================== 层级信息 ====================

    /** 章节编码（ICD-10章节，如 A00-B99） */
    @Column(name = "chapter_code", length = 16)
    private String chapterCode;

    /** 章节名称 */
    @Column(name = "chapter_name", length = 128)
    private String chapterName;

    /** 类目编码 */
    @Column(name = "category_code", length = 16)
    private String categoryCode;

    /** 父诊断ID */
    @Column(name = "parent_id")
    private Long parentId;

    /** 层级 */
    @Column(name = "level")
    private Integer level = 1;

    // ==================== 分类标记 ====================

    /** 是否主诊断 */
    @Column(name = "is_main")
    private Boolean isMain = true;

    /** 诊断类型（主诊断/次诊断/并发症） */
    @Column(name = "diagnosis_type", length = 32)
    private String diagnosisType;

    /** 性别限制（男/女/ALL） */
    @Column(name = "gender_restriction", length = 8)
    private String genderRestriction;

    // ==================== 科室关联 ====================

    /** 关联科室编码 */
    @Column(name = "related_dept_codes", columnDefinition = "text[]")
    private String[] relatedDeptCodes;

    // ==================== 状态与扩展 ====================

    /** 状态：ACTIVE/INACTIVE/RETIRED */
    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    /** 严重程度 */
    @Column(name = "severity_level", length = 16)
    private String severityLevel;

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