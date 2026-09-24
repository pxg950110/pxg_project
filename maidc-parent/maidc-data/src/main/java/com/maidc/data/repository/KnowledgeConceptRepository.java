package com.maidc.data.repository;

import com.maidc.data.entity.KnowledgeConceptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeConceptRepository extends JpaRepository<KnowledgeConceptEntity, Long> {
    List<KnowledgeConceptEntity> findByKnowledgeIdAndIsDeletedFalse(Long knowledgeId);
    List<KnowledgeConceptEntity> findByConceptIdAndIsDeletedFalse(Long conceptId);
    void deleteByKnowledgeId(Long knowledgeId);
}
