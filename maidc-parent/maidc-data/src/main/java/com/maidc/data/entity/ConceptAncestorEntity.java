package com.maidc.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "m_concept_ancestor", schema = "masterdata")
public class ConceptAncestorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ancestor_concept_id", nullable = false)
    private Long ancestorConceptId;

    @Column(name = "descendant_concept_id", nullable = false)
    private Long descendantConceptId;

    @Column(name = "min_levels_of_separation", nullable = false)
    private Integer minLevelsOfSeparation = 0;

    @Column(name = "max_levels_of_separation", nullable = false)
    private Integer maxLevelsOfSeparation = 0;
}
