package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "c_disease_kb_space", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_disease_kb_space SET is_deleted = true WHERE id = ?")
public class DiseaseKbSpaceEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** ICD-10 编码绑定，用于队列自动推荐 */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "icd_codes", columnDefinition = "text[]")
    private String[] icdCodes;

    /** 关联专病队列 cdr.c_disease_cohort.id（可空） */
    @Column(name = "cohort_id")
    private Long cohortId;

    @Column(name = "icon_color", length = 16)
    private String iconColor = "#2d5afa";

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";
}
