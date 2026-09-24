package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.PropertyEntity;
import com.maidc.data.repository.PropertyRepository;
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
public class PropertyService {

    private final PropertyRepository propertyRepository;

    public Page<PropertyEntity> list(String keyword, int page, int size) {
        Specification<PropertyEntity> spec = (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
                    cb.like(root.get("code"), "%" + keyword + "%")
            );
        };
        return propertyRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "code")));
    }

    public List<PropertyEntity> listAll() {
        return propertyRepository.findByIsDeletedFalse();
    }

    public List<PropertyEntity> listByParent(Long parentId) {
        return propertyRepository.findByParentIdAndIsDeletedFalse(parentId);
    }

    public PropertyEntity getById(Long id) {
        return propertyRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "特性不存在: " + id));
    }

    public PropertyEntity getByCode(String code) {
        return propertyRepository.findByCodeAndIsDeletedFalse(code)
                .orElseThrow(() -> new BusinessException(404, "特性不存在: " + code));
    }

    @Transactional
    public PropertyEntity create(PropertyEntity entity) {
        if (propertyRepository.existsByCodeAndIsDeletedFalse(entity.getCode())) {
            throw new BusinessException(400, "特性代码已存在: " + entity.getCode());
        }
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        PropertyEntity saved = propertyRepository.save(entity);
        log.info("特性创建成功: id={}, code={}", saved.getId(), saved.getCode());
        return saved;
    }

    @Transactional
    public PropertyEntity update(Long id, PropertyEntity updates) {
        PropertyEntity entity = getById(id);
        if (updates.getCode() != null && !updates.getCode().equals(entity.getCode())) {
            if (propertyRepository.existsByCodeAndIsDeletedFalse(updates.getCode())) {
                throw new BusinessException(400, "特性代码已存在: " + updates.getCode());
            }
            entity.setCode(updates.getCode());
        }
        if (updates.getName() != null) entity.setName(updates.getName());
        if (updates.getNameEn() != null) entity.setNameEn(updates.getNameEn());
        if (updates.getDefinition() != null) entity.setDefinition(updates.getDefinition());
        if (updates.getConceptType() != null) entity.setConceptType(updates.getConceptType());
        if (updates.getParentId() != null) entity.setParentId(updates.getParentId());
        if (updates.getVersion() != null) entity.setVersion(updates.getVersion());
        if (updates.getStatus() != null) entity.setStatus(updates.getStatus());
        return propertyRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        PropertyEntity entity = getById(id);
        List<PropertyEntity> children = propertyRepository.findByParentIdAndIsDeletedFalse(id);
        if (!children.isEmpty()) {
            throw new BusinessException(400, "特性下存在 " + children.size() + " 个子特性，无法删除");
        }
        propertyRepository.delete(entity);
        log.info("特性已删除: id={}, code={}", id, entity.getCode());
    }
}