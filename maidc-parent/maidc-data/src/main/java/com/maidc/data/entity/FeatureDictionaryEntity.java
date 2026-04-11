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
@Table(name = "r_feature_dictionary", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_feature_dictionary SET is_deleted = true WHERE id = ?")
public class FeatureDictionaryEntity extends BaseEntity {

    @Column(name = "feature_name", nullable = false, length = 128)
    private String featureName;

    @Column(name = "feature_category", nullable = false, length = 32)
    private String featureCategory;

    @Column(name = "data_type", nullable = false, length = 32)
    private String dataType;

    @Column(name = "standard_code", length = 64)
    private String standardCode;

    @Column(name = "standard_system", length = 32)
    private String standardSystem;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;
}
