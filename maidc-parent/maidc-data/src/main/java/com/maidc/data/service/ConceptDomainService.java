package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.enums.ErrorCode;
import com.maidc.data.entity.ConceptDomainEntity;
import com.maidc.data.entity.ValueMeaningEntity;
import com.maidc.data.repository.ConceptDomainRepository;
import com.maidc.data.repository.ValueMeaningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 概念域（WS/T 303-2023 CD）服务：概念域 CRUD 与其值含义（Value Meaning）子资源。
 */
@Service
@RequiredArgsConstructor
public class ConceptDomainService {

    private final ConceptDomainRepository conceptDomainRepository;
    private final ValueMeaningRepository valueMeaningRepository;

    public Page<ConceptDomainEntity> list(String keyword, String domainType, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.ASC, "code"));
        if (keyword != null && !keyword.isBlank()) {
            if (domainType != null && !domainType.isBlank()) {
                return conceptDomainRepository.searchByKeywordAndType(keyword, domainType, pageable);
            }
            return conceptDomainRepository.searchByKeyword(keyword, pageable);
        }
        if (domainType != null && !domainType.isBlank()) {
            return conceptDomainRepository.findByDomainTypeAndIsDeletedFalse(domainType, pageable);
        }
        return conceptDomainRepository.findByIsDeletedFalse(pageable);
    }

    public List<ConceptDomainEntity> listAll() {
        return conceptDomainRepository.findByIsDeletedFalse();
    }

    public ConceptDomainEntity getById(Long id) {
        return conceptDomainRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public ConceptDomainEntity create(ConceptDomainEntity entity) {
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            throw new BusinessException(400, "概念域代码不能为空");
        }
        if (conceptDomainRepository.existsByCodeAndIsDeletedFalse(entity.getCode())) {
            throw new BusinessException(400, "概念域代码已存在: " + entity.getCode());
        }
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        if (entity.getCreatedBy() == null || entity.getCreatedBy().isBlank()) entity.setCreatedBy("system");
        entity.setIsDeleted(false);
        return conceptDomainRepository.save(entity);
    }

    @Transactional
    public ConceptDomainEntity update(Long id, ConceptDomainEntity updates) {
        ConceptDomainEntity entity = getById(id);
        if (updates.getName() != null) {
            entity.setName(updates.getName());
        }
        if (updates.getNameEn() != null) {
            entity.setNameEn(updates.getNameEn());
        }
        if (updates.getDefinition() != null) {
            entity.setDefinition(updates.getDefinition());
        }
        if (updates.getDomainType() != null) {
            entity.setDomainType(updates.getDomainType());
        }
        if (updates.getDescriptionRule() != null) {
            entity.setDescriptionRule(updates.getDescriptionRule());
        }
        return conceptDomainRepository.save(entity);
    }

    /** 删除概念域（逻辑删除）并级联逻辑删除其值含义 */
    @Transactional
    public void delete(Long id) {
        ConceptDomainEntity entity = getById(id);
        entity.setIsDeleted(true);
        conceptDomainRepository.save(entity);
        valueMeaningRepository.findByConceptDomainIdAndIsDeletedFalseOrderBySortOrder(id)
                .forEach(vm -> {
                    vm.setIsDeleted(true);
                    valueMeaningRepository.save(vm);
                });
    }

    // ---------- 值含义（Value Meaning）子资源 ----------

    public List<ValueMeaningEntity> listValueMeanings(Long conceptDomainId, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return valueMeaningRepository.searchByKeyword(conceptDomainId, keyword);
        }
        return valueMeaningRepository.findByConceptDomainIdAndIsDeletedFalseOrderBySortOrder(conceptDomainId);
    }

    @Transactional
    public ValueMeaningEntity addValueMeaning(Long conceptDomainId, ValueMeaningEntity vm) {
        getById(conceptDomainId);
        if (vm.getCode() == null || vm.getCode().isBlank()) {
            throw new BusinessException(400, "值含义代码不能为空");
        }
        if (valueMeaningRepository.existsByConceptDomainIdAndCodeAndIsDeletedFalse(conceptDomainId, vm.getCode())) {
            throw new BusinessException(400, "该概念域下值含义代码已存在: " + vm.getCode());
        }
        vm.setConceptDomainId(conceptDomainId);
        if (vm.getOrgId() == null) vm.setOrgId(0L);
        if (vm.getCreatedBy() == null || vm.getCreatedBy().isBlank()) vm.setCreatedBy("system");
        vm.setIsDeleted(false);
        return valueMeaningRepository.save(vm);
    }

    @Transactional
    public ValueMeaningEntity updateValueMeaning(Long id, ValueMeaningEntity updates) {
        ValueMeaningEntity vm = valueMeaningRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (updates.getName() != null) {
            vm.setName(updates.getName());
        }
        if (updates.getNameEn() != null) {
            vm.setNameEn(updates.getNameEn());
        }
        if (updates.getDefinition() != null) {
            vm.setDefinition(updates.getDefinition());
        }
        if (updates.getSortOrder() != null) {
            vm.setSortOrder(updates.getSortOrder());
        }
        return valueMeaningRepository.save(vm);
    }

    @Transactional
    public void deleteValueMeaning(Long id) {
        ValueMeaningEntity vm = valueMeaningRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        vm.setIsDeleted(true);
        valueMeaningRepository.save(vm);
    }
}
