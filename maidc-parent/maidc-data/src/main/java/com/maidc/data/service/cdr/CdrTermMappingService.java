package com.maidc.data.service.cdr;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.cdr.CdrTermMappingEntity;
import com.maidc.data.repository.cdr.CdrTermMappingRepository;
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
 * CDR术语映射服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdrTermMappingService {

    private final CdrTermMappingRepository mappingRepository;

    public Page<CdrTermMappingEntity> list(String sourceSystem, String termType,
                                             String standardSystem, String mappingStatus,
                                             String keyword, int page, int size) {
        Specification<CdrTermMappingEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (sourceSystem != null && !sourceSystem.isBlank()) {
                predicates.add(cb.equal(root.get("sourceSystem"), sourceSystem));
            }
            if (termType != null && !termType.isBlank()) {
                predicates.add(cb.equal(root.get("termType"), termType));
            }
            if (standardSystem != null && !standardSystem.isBlank()) {
                predicates.add(cb.equal(root.get("standardSystem"), standardSystem));
            }
            if (mappingStatus != null && !mappingStatus.isBlank()) {
                predicates.add(cb.equal(root.get("mappingStatus"), mappingStatus));
            }
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.or(
                    cb.like(root.get("localCode"), "%" + keyword + "%"),
                    cb.like(root.get("localName"), "%" + keyword + "%"),
                    cb.like(root.get("standardCode"), "%" + keyword + "%"),
                    cb.like(root.get("standardName"), "%" + keyword + "%")
                ));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return mappingRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "confidence")));
    }

    public CdrTermMappingEntity getById(Long id) {
        return mappingRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "术语映射不存在: " + id));
    }

    public List<CdrTermMappingEntity> getByLocalTerm(String sourceSystem, String termType, String localCode) {
        return mappingRepository.findBySourceSystemAndTermTypeAndLocalCodeAndIsDeletedFalse(sourceSystem, termType, localCode)
                .stream().toList();
    }

    public List<CdrTermMappingEntity> searchByLocalName(String localName, String standardSystem) {
        return mappingRepository.searchByLocalNameAndStandardSystem(localName, standardSystem);
    }

    public List<CdrTermMappingEntity> getPendingMappings() {
        return mappingRepository.findByMappingStatusAndIsDeletedFalse("PENDING");
    }

    @Transactional
    public CdrTermMappingEntity create(CdrTermMappingEntity entity) {
        if (entity.getMappingStatus() == null) entity.setMappingStatus("PENDING");
        if (entity.getMappingType() == null) entity.setMappingType("MANUAL");
        if (entity.getIsPreferred() == null) entity.setIsPreferred(false);
        if (entity.getUseCount() == null) entity.setUseCount(0);
        entity.setOrgId(0L);
        CdrTermMappingEntity saved = mappingRepository.save(entity);
        log.info("术语映射创建: id={}, local={}, standard={}", saved.getId(), saved.getLocalCode(), saved.getStandardCode());
        return saved;
    }

    @Transactional
    public CdrTermMappingEntity update(Long id, CdrTermMappingEntity updates) {
        CdrTermMappingEntity entity = getById(id);
        if (updates.getStandardSystem() != null) entity.setStandardSystem(updates.getStandardSystem());
        if (updates.getStandardCode() != null) entity.setStandardCode(updates.getStandardCode());
        if (updates.getStandardName() != null) entity.setStandardName(updates.getStandardName());
        if (updates.getMappingType() != null) entity.setMappingType(updates.getMappingType());
        if (updates.getConfidence() != null) entity.setConfidence(updates.getConfidence());
        if (updates.getIsPreferred() != null) entity.setIsPreferred(updates.getIsPreferred());
        if (updates.getNotes() != null) entity.setNotes(updates.getNotes());
        return mappingRepository.save(entity);
    }

    @Transactional
    public CdrTermMappingEntity validate(Long id, String validatedBy) {
        CdrTermMappingEntity entity = getById(id);
        mappingRepository.updateMappingStatus(id, "VALIDATED", validatedBy);
        entity.setMappingStatus("VALIDATED");
        entity.setValidatedBy(validatedBy);
        entity.setValidatedAt(LocalDateTime.now());
        log.info("术语映射已验证: id={}, by={}", id, validatedBy);
        return mappingRepository.save(entity);
    }

    @Transactional
    public CdrTermMappingEntity confirm(Long id, String validatedBy) {
        CdrTermMappingEntity entity = getById(id);
        mappingRepository.updateMappingStatus(id, "CONFIRMED", validatedBy);
        entity.setMappingStatus("CONFIRMED");
        entity.setValidatedBy(validatedBy);
        entity.setValidatedAt(LocalDateTime.now());
        log.info("术语映射已确认: id={}, by={}", id, validatedBy);
        return mappingRepository.save(entity);
    }

    @Transactional
    public CdrTermMappingEntity reject(Long id, String validatedBy) {
        CdrTermMappingEntity entity = getById(id);
        mappingRepository.updateMappingStatus(id, "REJECTED", validatedBy);
        entity.setMappingStatus("REJECTED");
        entity.setValidatedBy(validatedBy);
        entity.setValidatedAt(LocalDateTime.now());
        log.info("术语映射已拒绝: id={}, by={}", id, validatedBy);
        return mappingRepository.save(entity);
    }

    @Transactional
    public void recordUsage(Long mappingId) {
        mappingRepository.incrementUseCount(mappingId);
    }

    public CdrTermMappingEntity findBestMatch(String sourceSystem, String termType, String localCode) {
        List<CdrTermMappingEntity> mappings = getByLocalTerm(sourceSystem, termType, localCode);
        return mappings.stream()
                .filter(m -> "CONFIRMED".equals(m.getMappingStatus()) || "VALIDATED".equals(m.getMappingStatus()))
                .max((a, b) -> {
                    if (a.getIsPreferred() && !b.getIsPreferred()) return 1;
                    if (!a.getIsPreferred() && b.getIsPreferred()) return -1;
                    return a.getConfidence().compareTo(b.getConfidence());
                })
                .orElse(null);
    }

    public Map<String, Object> getStats() {
        long pending = mappingRepository.countPendingMappings();
        long confirmed = mappingRepository.countConfirmedMappings();
        long total = mappingRepository.count();
        List<Object[]> byStandard = mappingRepository.countByStandardSystem();
        return Map.of(
            "total", total,
            "pending", pending,
            "confirmed", confirmed,
            "byStandardSystem", byStandard
        );
    }
}