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

import java.time.LocalDate;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "m_concept", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_concept SET is_deleted = true WHERE id = ?")
public class ConceptEntity extends BaseEntity {

    @Column(name = "concept_code", nullable = false, length = 64)
    private String conceptCode;

    @Column(name = "code_system_id", nullable = false)
    private Long codeSystemId;

    @Column(name = "name", nullable = false, length = 512)
    private String name;

    @Column(name = "name_en", length = 512)
    private String nameEn;

    @Column(name = "domain", length = 64)
    private String domain;

    @Column(name = "standard_class", length = 64)
    private String standardClass;

    @Column(name = "properties", columnDefinition = "jsonb")
    private String properties;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;
}
