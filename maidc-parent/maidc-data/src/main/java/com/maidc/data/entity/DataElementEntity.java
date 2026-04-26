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
@Table(name = "m_data_element", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_data_element SET is_deleted = true WHERE id = ?")
public class DataElementEntity extends BaseEntity {

    @Column(name = "element_code", nullable = false, length = 64)
    private String elementCode;

    @Column(name = "name", nullable = false, length = 256)
    private String name;

    @Column(name = "name_en", length = 256)
    private String nameEn;

    @Column(name = "definition", nullable = false, columnDefinition = "TEXT")
    private String definition;

    @Column(name = "object_class_name", length = 128)
    private String objectClassName;

    @Column(name = "object_class_id", length = 64)
    private String objectClassId;

    @Column(name = "property_name", length = 128)
    private String propertyName;

    @Column(name = "property_id", length = 64)
    private String propertyId;

    @Column(name = "data_type", nullable = false, length = 32)
    private String dataType;

    @Column(name = "representation_class", length = 32)
    private String representationClass;

    @Column(name = "value_domain_name", length = 128)
    private String valueDomainName;

    @Column(name = "value_domain_id", length = 64)
    private String valueDomainId;

    @Column(name = "min_length")
    private Integer minLength;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(name = "format", length = 64)
    private String format;

    @Column(name = "unit_of_measure", length = 32)
    private String unitOfMeasure;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "standard_source", length = 128)
    private String standardSource;

    @Column(name = "registration_status", nullable = false, length = 16)
    private String registrationStatus = "DRAFT";

    @Column(name = "version", nullable = false, length = 16)
    private String version = "1.0";

    @Column(name = "synonyms", columnDefinition = "TEXT[]")
    private String[] synonyms;

    @Column(name = "keywords", columnDefinition = "TEXT[]")
    private String[] keywords;

    @Column(name = "extra_attrs", columnDefinition = "jsonb")
    private String extraAttrs;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";
}
