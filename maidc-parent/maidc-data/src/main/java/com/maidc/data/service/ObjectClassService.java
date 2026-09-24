package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.ObjectClassEntity;
import com.maidc.data.repository.ObjectClassRepository;
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
public class ObjectClassService {

    private final ObjectClassRepository objectClassRepository;

    public Page<ObjectClassEntity> list(String keyword, int page, int size) {
        Specification<ObjectClassEntity> spec = (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
                    cb.like(root.get("code"), "%" + keyword + "%")
            );
        };
        return objectClassRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "code")));
    }

    public List<ObjectClassEntity> listAll() {
        return objectClassRepository.findByIsDeletedFalse();
    }

    public List<ObjectClassEntity> listByParent(Long parentId) {
        return objectClassRepository.findByParentIdAndIsDeletedFalse(parentId);
    }

    public ObjectClassEntity getById(Long id) {
        return objectClassRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "对象类不存在: " + id));
    }

    public ObjectClassEntity getByCode(String code) {
        return objectClassRepository.findByCodeAndIsDeletedFalse(code)
                .orElseThrow(() -> new BusinessException(404, "对象类不存在: " + code));
    }

    @Transactional
    public ObjectClassEntity create(ObjectClassEntity entity) {
        if (objectClassRepository.existsByCodeAndIsDeletedFalse(entity.getCode())) {
            throw new BusinessException(400, "对象类代码已存在: " + entity.getCode());
        }
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        ObjectClassEntity saved = objectClassRepository.save(entity);
        log.info("对象类创建成功: id={}, code={}", saved.getId(), saved.getCode());
        return saved;
    }

    @Transactional
    public ObjectClassEntity update(Long id, ObjectClassEntity updates) {
        ObjectClassEntity entity = getById(id);
        if (updates.getCode() != null && !updates.getCode().equals(entity.getCode())) {
            if (objectClassRepository.existsByCodeAndIsDeletedFalse(updates.getCode())) {
                throw new BusinessException(400, "对象类代码已存在: " + updates.getCode());
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
        return objectClassRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        ObjectClassEntity entity = getById(id);
        List<ObjectClassEntity> children = objectClassRepository.findByParentIdAndIsDeletedFalse(id);
        if (!children.isEmpty()) {
            throw new BusinessException(400, "对象类下存在 " + children.size() + " 个子对象类，无法删除");
        }
        objectClassRepository.delete(entity);
        log.info("对象类已删除: id={}, code={}", id, entity.getCode());
    }
}