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
@Table(name = "m_drug_interaction", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_drug_interaction SET is_deleted = true WHERE id = ?")
public class DrugInteractionEntity extends BaseEntity {

    @Column(name = "drug_concept_id_1", nullable = false)
    private Long drugConceptId1;

    @Column(name = "drug_concept_id_2", nullable = false)
    private Long drugConceptId2;

    @Column(name = "severity", nullable = false, length = 16)
    private String severity;

    @Column(name = "interaction_type", length = 32)
    private String interactionType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "evidence_level", length = 16)
    private String evidenceLevel;

    @Column(name = "clinical_action", columnDefinition = "TEXT")
    private String clinicalAction;
}
