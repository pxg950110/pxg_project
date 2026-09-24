package com.maidc.data.service.cdr;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.cdr.CdrQualityRuleEntity;
import com.maidc.data.repository.cdr.CdrQualityRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CDR数据质量规则服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdrQualityRuleService {

    private final CdrQualityRuleRepository ruleRepository;

    public Page<CdrQualityRuleEntity> list(String category, String targetSchema, String targetTable,
                                            Boolean enabled, int page, int size) {
        Specification<CdrQualityRuleEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("ruleCategory"), category));
            }
            if (targetSchema != null && !targetSchema.isBlank()) {
                predicates.add(cb.equal(root.get("targetSchema"), targetSchema));
            }
            if (targetTable != null && !targetTable.isBlank()) {
                predicates.add(cb.equal(root.get("targetTable"), targetTable));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return ruleRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "priority")));
    }

    public CdrQualityRuleEntity getById(Long id) {
        return ruleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "质量规则不存在: " + id));
    }

    public CdrQualityRuleEntity getByCode(String ruleCode) {
        return ruleRepository.findByRuleCodeAndIsDeletedFalse(ruleCode)
                .orElseThrow(() -> new BusinessException(404, "质量规则不存在: " + ruleCode));
    }

    @Transactional
    public CdrQualityRuleEntity create(CdrQualityRuleEntity entity) {
        if (ruleRepository.existsByRuleCodeAndIsDeletedFalse(entity.getRuleCode())) {
            throw new BusinessException(400, "规则编码已存在: " + entity.getRuleCode());
        }
        if (entity.getPriority() == null) entity.setPriority(100);
        if (entity.getEnabled() == null) entity.setEnabled(true);
        if (entity.getExecutionCount() == null) entity.setExecutionCount(0);
        entity.setOrgId(0L);
        CdrQualityRuleEntity saved = ruleRepository.save(entity);
        log.info("质量规则创建: id={}, code={}", saved.getId(), saved.getRuleCode());
        return saved;
    }

    @Transactional
    public CdrQualityRuleEntity update(Long id, CdrQualityRuleEntity updates) {
        CdrQualityRuleEntity entity = getById(id);
        if (updates.getRuleCode() != null && !updates.getRuleCode().equals(entity.getRuleCode())) {
            if (ruleRepository.existsByRuleCodeAndIsDeletedFalse(updates.getRuleCode())) {
                throw new BusinessException(400, "规则编码已存在: " + updates.getRuleCode());
            }
            entity.setRuleCode(updates.getRuleCode());
        }
        if (updates.getRuleName() != null) entity.setRuleName(updates.getRuleName());
        if (updates.getRuleCategory() != null) entity.setRuleCategory(updates.getRuleCategory());
        if (updates.getRuleType() != null) entity.setRuleType(updates.getRuleType());
        if (updates.getTargetSchema() != null) entity.setTargetSchema(updates.getTargetSchema());
        if (updates.getTargetTable() != null) entity.setTargetTable(updates.getTargetTable());
        if (updates.getTargetColumn() != null) entity.setTargetColumn(updates.getTargetColumn());
        if (updates.getRuleExpression() != null) entity.setRuleExpression(updates.getRuleExpression());
        if (updates.getSeverity() != null) entity.setSeverity(updates.getSeverity());
        if (updates.getFailureAction() != null) entity.setFailureAction(updates.getFailureAction());
        if (updates.getFixExpression() != null) entity.setFixExpression(updates.getFixExpression());
        if (updates.getPriority() != null) entity.setPriority(updates.getPriority());
        if (updates.getEnabled() != null) entity.setEnabled(updates.getEnabled());
        if (updates.getDescription() != null) entity.setDescription(updates.getDescription());
        return ruleRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        CdrQualityRuleEntity entity = getById(id);
        entity.setEnabled(false);
        ruleRepository.save(entity);
        ruleRepository.delete(entity);
        log.info("质量规则已删除: id={}, code={}", id, entity.getRuleCode());
    }

    @Transactional
    public CdrQualityRuleEntity toggleEnabled(Long id, Boolean enabled) {
        CdrQualityRuleEntity entity = getById(id);
        entity.setEnabled(enabled);
        return ruleRepository.save(entity);
    }

    public List<CdrQualityRuleEntity> getEnabledRulesForTable(String schema, String table) {
        return ruleRepository.findByTargetSchemaAndTargetTableAndIsDeletedFalse(schema, table)
                .stream()
                .filter(r -> r.getEnabled())
                .sorted((a, b) -> a.getPriority().compareTo(b.getPriority()))
                .toList();
    }

    @Transactional
    public void recordExecution(Long ruleId) {
        ruleRepository.incrementExecutionCount(ruleId);
    }

    public List<String> getCategories() {
        return ruleRepository.findDistinctCategories();
    }

    public Map<String, Object> getStats() {
        long total = ruleRepository.count();
        long enabled = ruleRepository.countEnabledRules();
        return Map.of("total", total, "enabled", enabled, "disabled", total - enabled);
    }
}