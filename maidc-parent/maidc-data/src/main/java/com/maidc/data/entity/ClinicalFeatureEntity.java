package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "r_clinical_feature", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_clinical_feature SET is_deleted = true WHERE id = ?")
public class ClinicalFeatureEntity extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "feature_code", nullable = false, length = 64)
    private String featureCode;

    @Column(name = "feature_name", nullable = false, length = 128)
    private String featureName;

    @Column(name = "data_type", nullable = false, length = 32)
    private String dataType;

    @Column(name = "source_table", length = 64)
    private String sourceTable;

    @Column(name = "source_column", length = 64)
    private String sourceColumn;

    @Column(name = "unit", length = 32)
    private String unit;

    @Column(name = "value_range", columnDefinition = "jsonb")
    private String valueRange;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
