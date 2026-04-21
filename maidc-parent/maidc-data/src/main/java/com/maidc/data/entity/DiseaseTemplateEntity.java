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
@Table(name = "s_disease_template", schema = "system")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE system.s_disease_template SET is_deleted = true WHERE id = ?")
public class DiseaseTemplateEntity extends BaseEntity {

    @Column(name = "disease_name", nullable = false, length = 128)
    private String diseaseName;

    @Column(name = "icd_codes", columnDefinition = "text[]")
    private String[] icdCodes;

    @Column(name = "inclusion_template", nullable = false, columnDefinition = "jsonb")
    private String inclusionTemplate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_builtin", nullable = false)
    private Boolean isBuiltin = false;
}
