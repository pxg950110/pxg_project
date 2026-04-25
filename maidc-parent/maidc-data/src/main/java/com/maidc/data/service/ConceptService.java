package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.ConceptEntity;
import com.maidc.data.repository.ConceptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptService {

    private final ConceptRepository conceptRepository;

    public Page<ConceptEntity> list(Long codeSystemId, String domain, String keyword, int page, int size) {
        Specification<ConceptEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (codeSystemId != null) {
                predicates.add(cb.equal(root.get("codeSystemId"), codeSystemId));
            }
            if (domain != null && !domain.isBlank()) {
                predicates.add(cb.equal(root.get("domain"), domain));
            }
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.or(
                        cb.like(root.get("name"), "%" + keyword + "%"),
                        cb.like(root.get("nameEn"), "%" + keyword + "%"),
                        cb.like(root.get("conceptCode"), "%" + keyword + "%")
                ));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return conceptRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public ConceptEntity getById(Long id) {
        return conceptRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "概念不存在: " + id));
    }

    public Page<ConceptEntity> search(String keyword, Long codeSystemId, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(400, "搜索关键词不能为空");
        }
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (codeSystemId != null) {
            return conceptRepository.searchByKeywordAndSystem(keyword, codeSystemId, pageable);
        }
        return conceptRepository.searchByKeyword(keyword, pageable);
    }

    @Transactional
    public ConceptEntity create(ConceptEntity entity) {
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        ConceptEntity saved = conceptRepository.save(entity);
        log.info("概念创建成功: id={}, code={}", saved.getId(), saved.getConceptCode());
        return saved;
    }

    @Transactional
    public ConceptEntity update(Long id, ConceptEntity updates) {
        ConceptEntity entity = getById(id);
        if (updates.getConceptCode() != null) entity.setConceptCode(updates.getConceptCode());
        if (updates.getCodeSystemId() != null) entity.setCodeSystemId(updates.getCodeSystemId());
        if (updates.getName() != null) entity.setName(updates.getName());
        if (updates.getNameEn() != null) entity.setNameEn(updates.getNameEn());
        if (updates.getDomain() != null) entity.setDomain(updates.getDomain());
        if (updates.getStandardClass() != null) entity.setStandardClass(updates.getStandardClass());
        if (updates.getProperties() != null) entity.setProperties(updates.getProperties());
        if (updates.getStatus() != null) entity.setStatus(updates.getStatus());
        if (updates.getValidFrom() != null) entity.setValidFrom(updates.getValidFrom());
        if (updates.getValidTo() != null) entity.setValidTo(updates.getValidTo());
        return conceptRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        ConceptEntity entity = getById(id);
        entity.setStatus("RETIRED");
        conceptRepository.save(entity);
        conceptRepository.delete(entity);
        log.info("概念已删除(逻辑): id={}, code={}", id, entity.getConceptCode());
    }
}
