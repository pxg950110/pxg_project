package com.maidc.data.service.cdr;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.cdr.CdrDataLineageEntity;
import com.maidc.data.repository.cdr.CdrDataLineageRepository;
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
import java.util.UUID;

/**
 * CDR数据血缘服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdrDataLineageService {

    private final CdrDataLineageRepository lineageRepository;

    public Page<CdrDataLineageEntity> list(String targetSchema, String targetTable,
                                            String sourceSystem, int page, int size) {
        Specification<CdrDataLineageEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (targetSchema != null && !targetSchema.isBlank()) {
                predicates.add(cb.equal(root.get("targetSchema"), targetSchema));
            }
            if (targetTable != null && !targetTable.isBlank()) {
                predicates.add(cb.equal(root.get("targetTable"), targetTable));
            }
            if (sourceSystem != null && !sourceSystem.isBlank()) {
                predicates.add(cb.equal(root.get("sourceSystem"), sourceSystem));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return lineageRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "lastSyncTime")));
    }

    public CdrDataLineageEntity getById(Long id) {
        return lineageRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "数据血缘不存在: " + id));
    }

    public List<CdrDataLineageEntity> getByTargetColumn(String schema, String table, String column) {
        return lineageRepository.findByTargetSchemaAndTargetTableAndTargetColumnAndIsDeletedFalse(schema, table, column);
    }

    public List<CdrDataLineageEntity> getBySourceSystem(String system) {
        return lineageRepository.findBySourceSystemAndSourceTableAndIsDeletedFalse(system, null);
    }

    public List<CdrDataLineageEntity> getByEtlJob(Long etlJobId) {
        return lineageRepository.findByEtlJobIdAndIsDeletedFalse(etlJobId);
    }

    @Transactional
    public CdrDataLineageEntity create(CdrDataLineageEntity entity) {
        if (entity.getLineageCode() == null || entity.getLineageCode().isBlank()) {
            entity.setLineageCode("L-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (lineageRepository.existsByLineageCodeAndIsDeletedFalse(entity.getLineageCode())) {
            throw new BusinessException(400, "血缘编码已存在: " + entity.getLineageCode());
        }
        if (entity.getSyncCount() == null) entity.setSyncCount(0);
        if (entity.getIsActive() == null) entity.setIsActive(true);
        entity.setOrgId(0L);
        CdrDataLineageEntity saved = lineageRepository.save(entity);
        log.info("数据血缘创建: id={}, code={}", saved.getId(), saved.getLineageCode());
        return saved;
    }

    @Transactional
    public CdrDataLineageEntity update(Long id, CdrDataLineageEntity updates) {
        CdrDataLineageEntity entity = getById(id);
        if (updates.getTransformType() != null) entity.setTransformType(updates.getTransformType());
        if (updates.getTransformExpression() != null) entity.setTransformExpression(updates.getTransformExpression());
        if (updates.getTransformLogic() != null) entity.setTransformLogic(updates.getTransformLogic());
        if (updates.getQualityCheckId() != null) entity.setQualityCheckId(updates.getQualityCheckId());
        if (updates.getQualityPassed() != null) entity.setQualityPassed(updates.getQualityPassed());
        if (updates.getMetadata() != null) entity.setMetadata(updates.getMetadata());
        if (updates.getIsActive() != null) entity.setIsActive(updates.getIsActive());
        return lineageRepository.save(entity);
    }

    @Transactional
    public void recordSync(Long lineageId, Boolean qualityPassed) {
        lineageRepository.incrementSyncCount(lineageId);
        CdrDataLineageEntity entity = getById(lineageId);
        if (qualityPassed != null) entity.setQualityPassed(qualityPassed);
        entity.setLastSyncTime(LocalDateTime.now());
        lineageRepository.save(entity);
    }

    @Transactional
    public void deactivate(Long id) {
        CdrDataLineageEntity entity = getById(id);
        entity.setIsActive(false);
        lineageRepository.save(entity);
        log.info("数据血缘已停用: id={}", id);
    }

    public List<Map<String, Object>> traceBack(String schema, String table, String column) {
        List<CdrDataLineageEntity> lineages = getByTargetColumn(schema, table, column);
        return lineages.stream()
                .filter(l -> l.getIsActive())
                .map(l -> Map.<String, Object>of(
                    "lineageId", l.getId(),
                    "sourceSystem", l.getSourceSystem(),
                    "sourceTable", l.getSourceTable(),
                    "sourceColumn", l.getSourceColumn(),
                    "transformType", l.getTransformType() != null ? l.getTransformType() : "DIRECT",
                    "qualityPassed", l.getQualityPassed() != null ? l.getQualityPassed() : false,
                    "lastSyncTime", l.getLastSyncTime() != null ? l.getLastSyncTime().toString() : ""
                ))
                .toList();
    }

    public Map<String, Object> getStats() {
        List<Object[]> bySource = lineageRepository.countBySourceSystem();
        long total = lineageRepository.count();
        long active = lineageRepository.findAll().stream().filter(l -> l.getIsActive()).count();
        return Map.of(
            "total", total,
            "active", active,
            "bySourceSystem", bySource
        );
    }

    public List<String> getSourceSystems() {
        return lineageRepository.findDistinctSourceSystems();
    }
}