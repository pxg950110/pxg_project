package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.ConceptAncestorEntity;
import com.maidc.data.entity.ConceptRelationshipEntity;
import com.maidc.data.entity.ConceptSynonymEntity;
import com.maidc.data.repository.ConceptAncestorRepository;
import com.maidc.data.repository.ConceptRelationshipRepository;
import com.maidc.data.repository.ConceptSynonymRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptMappingService {

    private final ConceptRelationshipRepository relationshipRepository;
    private final ConceptAncestorRepository ancestorRepository;
    private final ConceptSynonymRepository synonymRepository;

    /**
     * Get direct children: find IS_A relationships where concept_id_2 = conceptId (parent),
     * return relationships pointing to concept_id_1 (children).
     */
    public List<ConceptRelationshipEntity> getChildren(Long conceptId) {
        return relationshipRepository.findByConceptId2AndRelationshipTypeAndIsDeletedFalse(conceptId, "IS_A");
    }

    /**
     * Get all descendants via ancestor closure table.
     */
    public List<ConceptAncestorEntity> getDescendants(Long conceptId) {
        return ancestorRepository.findByAncestorConceptId(conceptId);
    }

    /**
     * Get all ancestors via ancestor closure table.
     */
    public List<ConceptAncestorEntity> getAncestors(Long conceptId) {
        return ancestorRepository.findByDescendantConceptId(conceptId);
    }

    /**
     * Get MAPS_TO relationships from this concept.
     */
    public List<ConceptRelationshipEntity> getMappings(Long conceptId) {
        return relationshipRepository.findByConceptId1AndRelationshipTypeAndIsDeletedFalse(conceptId, "MAPS_TO");
    }

    /**
     * Get synonyms for a concept.
     */
    public List<ConceptSynonymEntity> getSynonyms(Long conceptId) {
        return synonymRepository.findByConceptIdAndIsDeletedFalse(conceptId);
    }

    /**
     * Create a single mapping relationship.
     */
    @Transactional
    public ConceptRelationshipEntity createMapping(Long sourceId, Long targetId, String type) {
        if (sourceId == null || targetId == null || type == null || type.isBlank()) {
            throw new BusinessException(400, "源概念ID、目标概念ID和关系类型不能为空");
        }
        ConceptRelationshipEntity entity = new ConceptRelationshipEntity();
        entity.setConceptId1(sourceId);
        entity.setConceptId2(targetId);
        entity.setRelationshipType(type);
        entity.setIsHierarchical("IS_A".equals(type));
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        ConceptRelationshipEntity saved = relationshipRepository.save(entity);
        log.info("概念映射创建成功: id={}, {} -[{}]-> {}", saved.getId(), sourceId, type, targetId);
        return saved;
    }

    /**
     * Batch create mapping relationships.
     */
    @Transactional
    public List<ConceptRelationshipEntity> batchCreateMappings(List<ConceptRelationshipEntity> entities) {
        entities.forEach(e -> {
            if (e.getOrgId() == null) e.setOrgId(0L);
            if (e.getIsHierarchical() == null) {
                e.setIsHierarchical("IS_A".equals(e.getRelationshipType()));
            }
        });
        List<ConceptRelationshipEntity> saved = relationshipRepository.saveAll(entities);
        log.info("批量概念映射创建成功: count={}", saved.size());
        return saved;
    }

    /**
     * Logically delete a mapping relationship.
     */
    @Transactional
    public void deleteMapping(Long id) {
        ConceptRelationshipEntity entity = relationshipRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "概念关系不存在: " + id));
        relationshipRepository.delete(entity);
        log.info("概念关系已删除(逻辑): id={}", id);
    }
}
