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
@Table(name = "m_concept_synonym", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_concept_synonym SET is_deleted = true WHERE id = ?")
public class ConceptSynonymEntity extends BaseEntity {

    @Column(name = "concept_id", nullable = false)
    private Long conceptId;

    @Column(name = "synonym", nullable = false, length = 512)
    private String synonym;

    @Column(name = "language_code", nullable = false, length = 8)
    private String languageCode = "zh";

    @Column(name = "is_preferred", nullable = false)
    private Boolean isPreferred = false;
}
