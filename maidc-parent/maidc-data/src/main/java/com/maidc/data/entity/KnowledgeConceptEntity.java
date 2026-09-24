package com.maidc.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "m_knowledge_concept", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_knowledge_concept SET is_deleted = true WHERE id = ?")
public class KnowledgeConceptEntity {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "knowledge_id", nullable = false)
    private Long knowledgeId;

    @Column(name = "concept_id", nullable = false)
    private Long conceptId;

    @Column(name = "relevance", nullable = false, length = 16)
    private String relevance = "RELATED";

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy = "system";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "org_id", nullable = false)
    private Long orgId = 0L;
}
