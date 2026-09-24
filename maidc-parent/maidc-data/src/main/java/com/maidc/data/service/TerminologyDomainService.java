package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.TerminologyDomainEntity;
import com.maidc.data.repository.TerminologyDomainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerminologyDomainService {

    private final TerminologyDomainRepository domainRepository;

    public List<TerminologyDomainEntity> listActive() {
        return domainRepository.findByStatusAndIsDeletedFalseOrderBySortOrder("ACTIVE");
    }

    public List<TerminologyDomainEntity> listAll() {
        return domainRepository.findAllByIsDeletedFalseOrderBySortOrder();
    }

    public TerminologyDomainEntity getById(Long id) {
        return domainRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "术语领域不存在: " + id));
    }

    @Transactional
    public TerminologyDomainEntity create(TerminologyDomainEntity entity) {
        if (domainRepository.existsByCodeAndIsDeletedFalse(entity.getCode())) {
            throw new BusinessException(400, "领域代码已存在: " + entity.getCode());
        }
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        TerminologyDomainEntity saved = domainRepository.save(entity);
        log.info("术语领域创建成功: id={}, code={}", saved.getId(), saved.getCode());
        return saved;
    }

    @Transactional
    public TerminologyDomainEntity update(Long id, TerminologyDomainEntity updates) {
        TerminologyDomainEntity entity = getById(id);
        if (updates.getName() != null) entity.setName(updates.getName());
        if (updates.getNameEn() != null) entity.setNameEn(updates.getNameEn());
        if (updates.getDescription() != null) entity.setDescription(updates.getDescription());
        if (updates.getIcon() != null) entity.setIcon(updates.getIcon());
        if (updates.getSortOrder() != null) entity.setSortOrder(updates.getSortOrder());
        if (updates.getStatus() != null) entity.setStatus(updates.getStatus());
        TerminologyDomainEntity saved = domainRepository.save(entity);
        log.info("术语领域更新成功: id={}, code={}", saved.getId(), saved.getCode());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        TerminologyDomainEntity entity = getById(id);
        domainRepository.delete(entity);
        log.info("术语领域已删除(逻辑): id={}, code={}", id, entity.getCode());
    }
}
