package com.maidc.data.service;

import com.maidc.data.entity.DesensitizeRuleEntity;
import com.maidc.data.repository.DesensitizeRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesensitizeRuleService {

    private final DesensitizeRuleRepository desensitizeRuleRepository;

    public DesensitizeRuleEntity getDesensitizeRule(Long id) {
        return desensitizeRuleRepository.findById(id).orElse(null);
    }

    public Page<DesensitizeRuleEntity> listDesensitizeRules(int page, int size) {
        return desensitizeRuleRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public DesensitizeRuleEntity createDesensitizeRule(DesensitizeRuleEntity entity) {
        return desensitizeRuleRepository.save(entity);
    }

    @Transactional
    public DesensitizeRuleEntity updateDesensitizeRule(Long id, DesensitizeRuleEntity entity) {
        DesensitizeRuleEntity existing = desensitizeRuleRepository.findById(id).orElse(null);
        if (existing == null) return null;
        entity.setId(id);
        entity.setCreatedBy(existing.getCreatedBy());
        entity.setCreatedAt(existing.getCreatedAt());
        return desensitizeRuleRepository.save(entity);
    }

    @Transactional
    public void deleteDesensitizeRule(Long id) {
        desensitizeRuleRepository.deleteById(id);
    }

    @Transactional
    public DesensitizeRuleEntity toggleDesensitizeRule(Long id, boolean enabled) {
        DesensitizeRuleEntity entity = desensitizeRuleRepository.findById(id).orElse(null);
        if (entity == null) return null;
        entity.setEnabled(enabled);
        return desensitizeRuleRepository.save(entity);
    }
}
