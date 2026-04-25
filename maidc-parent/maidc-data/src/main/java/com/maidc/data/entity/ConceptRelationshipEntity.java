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
@Table(name = "m_concept_relationship", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_concept_relationship SET is_deleted = true WHERE id = ?")
public class ConceptRelationshipEntity extends BaseEntity {

    @Column(name = "concept_id_1", nullable = false)
    private Long conceptId1;

    @Column(name = "concept_id_2", nullable = false)
    private Long conceptId2;

    @Column(name = "relationship_type", nullable = false, length = 64)
    private String relationshipType;

    @Column(name = "is_hierarchical", nullable = false)
    private Boolean isHierarchical = false;
}
