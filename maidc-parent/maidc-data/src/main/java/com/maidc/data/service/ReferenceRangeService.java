package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.ReferenceRangeEntity;
import com.maidc.data.repository.ReferenceRangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferenceRangeService {

    private final ReferenceRangeRepository referenceRangeRepository;

    public List<ReferenceRangeEntity> list(Long conceptId, String gender) {
        if (conceptId != null) {
            return referenceRangeRepository.findByConceptIdAndIsDeletedFalse(conceptId);
        }
        return referenceRangeRepository.findAllByIsDeletedFalse();
    }

    public ReferenceRangeEntity getById(Long id) {
        return referenceRangeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "参考范围不存在: " + id));
    }

    public ReferenceRangeEntity evaluate(Long conceptId, String gender, BigDecimal age) {
        List<ReferenceRangeEntity> matches = referenceRangeRepository.findBestMatch(conceptId, gender, age);
        return matches.isEmpty() ? null : matches.get(0);
    }

    @Transactional
    public ReferenceRangeEntity create(ReferenceRangeEntity entity) {
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        ReferenceRangeEntity saved = referenceRangeRepository.save(entity);
        log.info("参考范围创建成功: id={}, conceptId={}", saved.getId(), saved.getConceptId());
        return saved;
    }

    @Transactional
    public ReferenceRangeEntity update(Long id, ReferenceRangeEntity updates) {
        ReferenceRangeEntity entity = getById(id);
        if (updates.getConceptId() != null) entity.setConceptId(updates.getConceptId());
        if (updates.getGender() != null) entity.setGender(updates.getGender());
        if (updates.getAgeMin() != null) entity.setAgeMin(updates.getAgeMin());
        if (updates.getAgeMax() != null) entity.setAgeMax(updates.getAgeMax());
        if (updates.getRangeLow() != null) entity.setRangeLow(updates.getRangeLow());
        if (updates.getRangeHigh() != null) entity.setRangeHigh(updates.getRangeHigh());
        if (updates.getUnit() != null) entity.setUnit(updates.getUnit());
        if (updates.getCriticalLow() != null) entity.setCriticalLow(updates.getCriticalLow());
        if (updates.getCriticalHigh() != null) entity.setCriticalHigh(updates.getCriticalHigh());
        if (updates.getSource() != null) entity.setSource(updates.getSource());
        return referenceRangeRepository.save(entity);
    }
}
