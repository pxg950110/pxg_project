package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.ConceptEntity;
import com.maidc.data.entity.KnowledgeCategoryEntity;
import com.maidc.data.entity.KnowledgeConceptEntity;
import com.maidc.data.entity.KnowledgeItemEntity;
import com.maidc.data.repository.ConceptRepository;
import com.maidc.data.repository.KnowledgeCategoryRepository;
import com.maidc.data.repository.KnowledgeConceptRepository;
import com.maidc.data.repository.KnowledgeItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeItemRepository itemRepository;
    private final KnowledgeCategoryRepository categoryRepository;
    private final KnowledgeConceptRepository knowledgeConceptRepository;
    private final ConceptRepository conceptRepository;

    // ── Categories ──

    public List<KnowledgeCategoryEntity> listCategories() {
        return categoryRepository.findByIsDeletedFalseOrderBySortOrder();
    }

    @Transactional
    public KnowledgeCategoryEntity createCategory(KnowledgeCategoryEntity entity) {
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        return categoryRepository.save(entity);
    }

    @Transactional
    public KnowledgeCategoryEntity updateCategory(Long id, KnowledgeCategoryEntity updates) {
        KnowledgeCategoryEntity entity = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "分类不存在: " + id));
        if (updates.getName() != null) entity.setName(updates.getName());
        if (updates.getParentId() != null) entity.setParentId(updates.getParentId());
        if (updates.getSortOrder() != null) entity.setSortOrder(updates.getSortOrder());
        return categoryRepository.save(entity);
    }

    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    // ── Knowledge Items ──

    public Page<KnowledgeItemEntity> search(Long categoryId, String itemType, String keyword, int page, int size) {
        PageRequest pr = PageRequest.of(page - 1, size);
        boolean hasCat = categoryId != null;
        boolean hasType = itemType != null && !itemType.isBlank();
        boolean hasKw = keyword != null && !keyword.isBlank();

        if (hasCat && hasType && hasKw) return itemRepository.searchByCategoryAndTypeAndKeyword(categoryId, itemType, keyword, pr);
        if (hasCat && hasType) return itemRepository.findByCategoryIdAndItemTypeAndIsDeletedFalse(categoryId, itemType, pr);
        if (hasCat && hasKw) return itemRepository.searchByCategoryAndKeyword(categoryId, keyword, pr);
        if (hasCat) return itemRepository.findByCategoryIdAndIsDeletedFalse(categoryId, pr);
        if (hasType && hasKw) return itemRepository.searchByTypeAndKeyword(itemType, keyword, pr);
        if (hasType) return itemRepository.findByItemTypeAndIsDeletedFalse(itemType, pr);
        if (hasKw) return itemRepository.searchByKeyword(keyword, pr);
        return itemRepository.findByIsDeletedFalse(pr);
    }

    public KnowledgeItemEntity getById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "知识条目不存在: " + id));
    }

    @Transactional
    public KnowledgeItemEntity create(KnowledgeItemEntity entity) {
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        KnowledgeItemEntity saved = itemRepository.save(entity);
        log.info("知识条目创建: id={}, title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    @Transactional
    public KnowledgeItemEntity update(Long id, KnowledgeItemEntity updates) {
        KnowledgeItemEntity entity = getById(id);
        if (updates.getTitle() != null) entity.setTitle(updates.getTitle());
        if (updates.getCategoryId() != null) entity.setCategoryId(updates.getCategoryId());
        if (updates.getItemType() != null) entity.setItemType(updates.getItemType());
        if (updates.getSummary() != null) entity.setSummary(updates.getSummary());
        if (updates.getContent() != null) entity.setContent(updates.getContent());
        if (updates.getSource() != null) entity.setSource(updates.getSource());
        if (updates.getAuthors() != null) entity.setAuthors(updates.getAuthors());
        if (updates.getPublishDate() != null) entity.setPublishDate(updates.getPublishDate());
        if (updates.getTags() != null) entity.setTags(updates.getTags());
        if (updates.getFileUrl() != null) entity.setFileUrl(updates.getFileUrl());
        if (updates.getFileName() != null) entity.setFileName(updates.getFileName());
        if (updates.getStatus() != null) entity.setStatus(updates.getStatus());
        return itemRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        itemRepository.deleteById(id);
    }

    // ── Concept Associations ──

    public List<Map<String, Object>> getConceptAssociations(Long knowledgeId) {
        List<KnowledgeConceptEntity> links = knowledgeConceptRepository.findByKnowledgeIdAndIsDeletedFalse(knowledgeId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (KnowledgeConceptEntity link : links) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", link.getId());
            item.put("conceptId", link.getConceptId());
            item.put("relevance", link.getRelevance());
            conceptRepository.findById(link.getConceptId()).ifPresent(c -> {
                item.put("conceptCode", c.getConceptCode());
                item.put("conceptName", c.getName());
            });
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getKnowledgeByConcept(Long conceptId) {
        List<KnowledgeConceptEntity> links = knowledgeConceptRepository.findByConceptIdAndIsDeletedFalse(conceptId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (KnowledgeConceptEntity link : links) {
            itemRepository.findById(link.getKnowledgeId()).ifPresent(ki -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", link.getId());
                item.put("knowledgeId", ki.getId());
                item.put("title", ki.getTitle());
                item.put("itemType", ki.getItemType());
                item.put("relevance", link.getRelevance());
                result.add(item);
            });
        }
        return result;
    }

    @Transactional
    public void associateConcept(Long knowledgeId, Long conceptId, String relevance) {
        KnowledgeItemEntity item = getById(knowledgeId);
        conceptRepository.findById(conceptId)
                .orElseThrow(() -> new BusinessException(404, "概念不存在: " + conceptId));
        KnowledgeConceptEntity link = new KnowledgeConceptEntity();
        link.setKnowledgeId(knowledgeId);
        link.setConceptId(conceptId);
        link.setRelevance(relevance != null ? relevance : "RELATED");
        link.setOrgId(0L);
        knowledgeConceptRepository.save(link);
    }

    @Transactional
    public void removeAssociation(Long id) {
        knowledgeConceptRepository.deleteById(id);
    }
}
