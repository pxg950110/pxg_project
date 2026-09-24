package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.DataElementConceptEntity;
import com.maidc.data.repository.DataElementConceptRepository;
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
public class DataElementConceptService {

    private final DataElementConceptRepository dataElementConceptRepository;

    public Page<DataElementConceptEntity> list(String keyword, String objectClassCode, String propertyCode, int page, int size) {
        Specification<DataElementConceptEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
                        cb.like(root.get("code"), "%" + keyword + "%")
                ));
            }
            if (objectClassCode != null && !objectClassCode.isBlank()) {
                predicates.add(cb.equal(root.get("objectClassCode"), objectClassCode));
            }
            if (propertyCode != null && !propertyCode.isBlank()) {
                predicates.add(cb.equal(root.get("propertyCode"), propertyCode));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return dataElementConceptRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public DataElementConceptEntity getById(Long id) {
        return dataElementConceptRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "数据元概念不存在: " + id));
    }

    public DataElementConceptEntity getByCode(String code) {
        return dataElementConceptRepository.findByCodeAndIsDeletedFalse(code)
                .orElseThrow(() -> new BusinessException(404, "数据元概念不存在: " + code));
    }

    @Transactional
    public DataElementConceptEntity create(DataElementConceptEntity entity) {
        if (dataElementConceptRepository.existsByCodeAndIsDeletedFalse(entity.getCode())) {
            throw new BusinessException(400, "数据元概念代码已存在: " + entity.getCode());
        }
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        DataElementConceptEntity saved = dataElementConceptRepository.save(entity);
        log.info("数据元概念创建成功: id={}, code={}", saved.getId(), saved.getCode());
        return saved;
    }

    @Transactional
    public DataElementConceptEntity update(Long id, DataElementConceptEntity updates) {
        DataElementConceptEntity entity = getById(id);
        if (updates.getCode() != null && !updates.getCode().equals(entity.getCode())) {
            if (dataElementConceptRepository.existsByCodeAndIsDeletedFalse(updates.getCode())) {
                throw new BusinessException(400, "数据元概念代码已存在: " + updates.getCode());
            }
            entity.setCode(updates.getCode());
        }
        if (updates.getName() != null) entity.setName(updates.getName());
        if (updates.getNameEn() != null) entity.setNameEn(updates.getNameEn());
        if (updates.getDefinition() != null) entity.setDefinition(updates.getDefinition());
        if (updates.getObjectClassCode() != null) entity.setObjectClassCode(updates.getObjectClassCode());
        if (updates.getObjectClassName() != null) entity.setObjectClassName(updates.getObjectClassName());
        if (updates.getObjectClassDefinition() != null) entity.setObjectClassDefinition(updates.getObjectClassDefinition());
        if (updates.getPropertyCode() != null) entity.setPropertyCode(updates.getPropertyCode());
        if (updates.getPropertyName() != null) entity.setPropertyName(updates.getPropertyName());
        if (updates.getPropertyDefinition() != null) entity.setPropertyDefinition(updates.getPropertyDefinition());
        if (updates.getConceptDomainId() != null) entity.setConceptDomainId(updates.getConceptDomainId());
        if (updates.getVersion() != null) entity.setVersion(updates.getVersion());
        if (updates.getStatus() != null) entity.setStatus(updates.getStatus());
        return dataElementConceptRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        DataElementConceptEntity entity = getById(id);
        dataElementConceptRepository.delete(entity);
        log.info("数据元概念已删除: id={}, code={}", id, entity.getCode());
    }
}