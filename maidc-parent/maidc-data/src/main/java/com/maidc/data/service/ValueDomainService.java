package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.PermissibleValueEntity;
import com.maidc.data.entity.ValueDomainEntity;
import com.maidc.data.repository.PermissibleValueRepository;
import com.maidc.data.repository.ValueDomainRepository;
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
public class ValueDomainService {

    private final ValueDomainRepository valueDomainRepository;
    private final PermissibleValueRepository permissibleValueRepository;

    public Page<ValueDomainEntity> list(String keyword, Long conceptDomainId, String domainType, int page, int size) {
        Specification<ValueDomainEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
                        cb.like(root.get("code"), "%" + keyword + "%")
                ));
            }
            if (conceptDomainId != null) {
                predicates.add(cb.equal(root.get("conceptDomainId"), conceptDomainId));
            }
            if (domainType != null && !domainType.isBlank()) {
                predicates.add(cb.equal(root.get("domainType"), domainType));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return valueDomainRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public ValueDomainEntity getById(Long id) {
        return valueDomainRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "值域不存在: " + id));
    }

    public ValueDomainEntity getByCode(String code) {
        return valueDomainRepository.findByCodeAndIsDeletedFalse(code)
                .orElseThrow(() -> new BusinessException(404, "值域不存在: " + code));
    }

    public List<PermissibleValueEntity> listPermissibleValues(Long valueDomainId) {
        ValueDomainEntity vd = getById(valueDomainId);
        if (!"ENUMERABLE".equals(vd.getDomainType())) {
            throw new BusinessException(400, "不可枚举值域没有允许值列表");
        }
        return permissibleValueRepository.findByValueDomainIdAndIsDeletedFalseOrderBySortOrder(valueDomainId);
    }

    @Transactional
    public ValueDomainEntity create(ValueDomainEntity entity) {
        if (valueDomainRepository.existsByCodeAndIsDeletedFalse(entity.getCode())) {
            throw new BusinessException(400, "值域代码已存在: " + entity.getCode());
        }
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        ValueDomainEntity saved = valueDomainRepository.save(entity);
        log.info("值域创建成功: id={}, code={}", saved.getId(), saved.getCode());
        return saved;
    }

    @Transactional
    public ValueDomainEntity update(Long id, ValueDomainEntity updates) {
        ValueDomainEntity entity = getById(id);
        if (updates.getCode() != null && !updates.getCode().equals(entity.getCode())) {
            if (valueDomainRepository.existsByCodeAndIsDeletedFalse(updates.getCode())) {
                throw new BusinessException(400, "值域代码已存在: " + updates.getCode());
            }
            entity.setCode(updates.getCode());
        }
        if (updates.getName() != null) entity.setName(updates.getName());
        if (updates.getNameEn() != null) entity.setNameEn(updates.getNameEn());
        if (updates.getDefinition() != null) entity.setDefinition(updates.getDefinition());
        if (updates.getDomainType() != null) entity.setDomainType(updates.getDomainType());
        if (updates.getDescription() != null) entity.setDescription(updates.getDescription());
        if (updates.getDataType() != null) entity.setDataType(updates.getDataType());
        if (updates.getUnitOfMeasure() != null) entity.setUnitOfMeasure(updates.getUnitOfMeasure());
        if (updates.getUnitName() != null) entity.setUnitName(updates.getUnitName());
        if (updates.getRepresentationClass() != null) entity.setRepresentationClass(updates.getRepresentationClass());
        if (updates.getFormat() != null) entity.setFormat(updates.getFormat());
        if (updates.getMinLength() != null) entity.setMinLength(updates.getMinLength());
        if (updates.getMaxLength() != null) entity.setMaxLength(updates.getMaxLength());
        if (updates.getConceptDomainId() != null) entity.setConceptDomainId(updates.getConceptDomainId());
        if (updates.getVersion() != null) entity.setVersion(updates.getVersion());
        if (updates.getStatus() != null) entity.setStatus(updates.getStatus());
        return valueDomainRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        ValueDomainEntity entity = getById(id);
        long pvCount = permissibleValueRepository.countByValueDomainIdAndIsDeletedFalse(id);
        if (pvCount > 0) {
            throw new BusinessException(400, "值域下存在 " + pvCount + " 个允许值，无法删除");
        }
        valueDomainRepository.delete(entity);
        log.info("值域已删除: id={}, code={}", id, entity.getCode());
    }

    // ==================== 允许值管理 ====================

    @Transactional
    public PermissibleValueEntity addPermissibleValue(Long valueDomainId, PermissibleValueEntity pv) {
        ValueDomainEntity vd = getById(valueDomainId);
        if (!"ENUMERABLE".equals(vd.getDomainType())) {
            throw new BusinessException(400, "不可枚举值域不能添加允许值");
        }
        if (permissibleValueRepository.existsByValueDomainIdAndValueAndIsDeletedFalse(valueDomainId, pv.getValue())) {
            throw new BusinessException(400, "允许值已存在: " + pv.getValue());
        }
        pv.setValueDomainId(valueDomainId);
        if (pv.getSortOrder() == null) {
            long count = permissibleValueRepository.countByValueDomainIdAndIsDeletedFalse(valueDomainId);
            pv.setSortOrder((int) count + 1);
        }
        PermissibleValueEntity saved = permissibleValueRepository.save(pv);
        log.info("允许值添加成功: id={}, value={}", saved.getId(), saved.getValue());
        return saved;
    }

    @Transactional
    public PermissibleValueEntity updatePermissibleValue(Long id, PermissibleValueEntity updates) {
        PermissibleValueEntity pv = permissibleValueRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "允许值不存在: " + id));
        if (updates.getValue() != null && !updates.getValue().equals(pv.getValue())) {
            if (permissibleValueRepository.existsByValueDomainIdAndValueAndIsDeletedFalse(pv.getValueDomainId(), updates.getValue())) {
                throw new BusinessException(400, "允许值已存在: " + updates.getValue());
            }
            pv.setValue(updates.getValue());
        }
        if (updates.getValueMeaningId() != null) pv.setValueMeaningId(updates.getValueMeaningId());
        if (updates.getValueMeaningName() != null) pv.setValueMeaningName(updates.getValueMeaningName());
        if (updates.getSortOrder() != null) pv.setSortOrder(updates.getSortOrder());
        if (updates.getIsActive() != null) pv.setIsActive(updates.getIsActive());
        if (updates.getEffectiveDate() != null) pv.setEffectiveDate(updates.getEffectiveDate());
        if (updates.getExpiryDate() != null) pv.setExpiryDate(updates.getExpiryDate());
        return permissibleValueRepository.save(pv);
    }

    @Transactional
    public void deletePermissibleValue(Long id) {
        PermissibleValueEntity pv = permissibleValueRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "允许值不存在: " + id));
        permissibleValueRepository.delete(pv);
        log.info("允许值已删除: id={}, value={}", id, pv.getValue());
    }

    @Transactional
    public List<PermissibleValueEntity> batchImportPermissibleValues(Long valueDomainId, List<PermissibleValueEntity> values) {
        ValueDomainEntity vd = getById(valueDomainId);
        if (!"ENUMERABLE".equals(vd.getDomainType())) {
            throw new BusinessException(400, "不可枚举值域不能导入允许值");
        }
        List<PermissibleValueEntity> saved = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            PermissibleValueEntity pv = values.get(i);
            pv.setValueDomainId(valueDomainId);
            if (pv.getSortOrder() == null) pv.setSortOrder(i + 1);
            saved.add(permissibleValueRepository.save(pv));
        }
        log.info("批量导入允许值成功: valueDomainId={}, count={}", valueDomainId, saved.size());
        return saved;
    }
}