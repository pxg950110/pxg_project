package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.InstitutionEntity;
import com.maidc.data.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstitutionService {

    private final InstitutionRepository institutionRepository;

    public List<InstitutionEntity> list() {
        return institutionRepository.findAll();
    }

    public InstitutionEntity getById(Long id) {
        return institutionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "机构不存在: " + id));
    }

    @Transactional
    public InstitutionEntity create(InstitutionEntity entity) {
        if (institutionRepository.existsByInstCodeAndIsDeletedFalse(entity.getInstCode())) {
            throw new BusinessException(400, "机构编码已存在: " + entity.getInstCode());
        }
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        InstitutionEntity saved = institutionRepository.save(entity);
        log.info("机构创建成功: id={}, instCode={}", saved.getId(), saved.getInstCode());
        return saved;
    }

    @Transactional
    public InstitutionEntity update(Long id, InstitutionEntity updates) {
        InstitutionEntity entity = getById(id);
        if (updates.getName() != null) entity.setName(updates.getName());
        if (updates.getShortName() != null) entity.setShortName(updates.getShortName());
        if (updates.getStatus() != null) entity.setStatus(updates.getStatus());
        return institutionRepository.save(entity);
    }
}
