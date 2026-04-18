package com.maidc.data.service;

import com.maidc.data.entity.DataQualityRuleEntity;
import com.maidc.data.repository.DataQualityRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataQualityRuleService {

    private final DataQualityRuleRepository dataQualityRuleRepository;

    public DataQualityRuleEntity getDataQualityRule(Long id) {
        return dataQualityRuleRepository.findById(id).orElse(null);
    }

    public Page<DataQualityRuleEntity> listDataQualityRules(int page, int size) {
        return dataQualityRuleRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public DataQualityRuleEntity createDataQualityRule(DataQualityRuleEntity entity) {
        return dataQualityRuleRepository.save(entity);
    }

    @Transactional
    public void deleteDataQualityRule(Long id) {
        dataQualityRuleRepository.deleteById(id);
    }

    @Transactional
    public DataQualityRuleEntity updateDataQualityRule(Long id, DataQualityRuleEntity entity) {
        DataQualityRuleEntity existing = dataQualityRuleRepository.findById(id).orElse(null);
        if (existing == null) return null;
        entity.setId(id);
        entity.setCreatedBy(existing.getCreatedBy());
        entity.setCreatedAt(existing.getCreatedAt());
        return dataQualityRuleRepository.save(entity);
    }

    @Transactional
    public DataQualityRuleEntity toggleDataQualityRule(Long id, boolean enabled) {
        DataQualityRuleEntity entity = dataQualityRuleRepository.findById(id).orElse(null);
        if (entity == null) return null;
        entity.setEnabled(enabled);
        return dataQualityRuleRepository.save(entity);
    }
}
