package com.maidc.data.service.cdr;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.cdr.CdrQuarantineDataEntity;
import com.maidc.data.repository.cdr.CdrQuarantineDataRepository;
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
 * CDR数据隔离区服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdrQuarantineService {

    private final CdrQuarantineDataRepository quarantineRepository;

    public Page<CdrQuarantineDataEntity> list(String status, String priority,
                                                String sourceTable, String assignedTo,
                                                int page, int size) {
        Specification<CdrQuarantineDataEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null && !priority.isBlank()) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (sourceTable != null && !sourceTable.isBlank()) {
                predicates.add(cb.equal(root.get("sourceTable"), sourceTable));
            }
            if (assignedTo != null && !assignedTo.isBlank()) {
                predicates.add(cb.equal(root.get("assignedTo"), assignedTo));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return quarantineRepository.findAll(spec, PageRequest.of(page - 1, size,
                Sort.by(Sort.Direction.DESC, "priority", "quarantineTime")));
    }

    public CdrQuarantineDataEntity getById(Long id) {
        return quarantineRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "隔离数据不存在: " + id));
    }

    public CdrQuarantineDataEntity getByCode(String quarantineCode) {
        return quarantineRepository.findByQuarantineCodeAndIsDeletedFalse(quarantineCode)
                .orElseThrow(() -> new BusinessException(404, "隔离数据不存在: " + quarantineCode));
    }

    public List<CdrQuarantineDataEntity> getPendingByPriority(String priority) {
        return quarantineRepository.findByPriorityAndStatusAndIsDeletedFalse(priority, "PENDING");
    }

    public List<CdrQuarantineDataEntity> getMyAssignments(String assignedTo) {
        return quarantineRepository.findByAssignedToAndStatusAndIsDeletedFalse(assignedTo, "ASSIGNED");
    }

    @Transactional
    public CdrQuarantineDataEntity create(CdrQuarantineDataEntity entity) {
        if (entity.getQuarantineCode() == null || entity.getQuarantineCode().isBlank()) {
            entity.setQuarantineCode("Q-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (quarantineRepository.existsByQuarantineCodeAndIsDeletedFalse(entity.getQuarantineCode())) {
            throw new BusinessException(400, "隔离编码已存在: " + entity.getQuarantineCode());
        }
        if (entity.getQuarantineTime() == null) entity.setQuarantineTime(LocalDateTime.now());
        if (entity.getErrorCount() == null) entity.setErrorCount(1);
        if (entity.getPriority() == null) entity.setPriority("MEDIUM");
        if (entity.getStatus() == null) entity.setStatus("PENDING");
        entity.setOrgId(0L);
        return quarantineRepository.save(entity);
    }

    @Transactional
    public CdrQuarantineDataEntity assign(Long id, String assignedTo) {
        CdrQuarantineDataEntity entity = getById(id);
        quarantineRepository.assignQuarantine(id, "ASSIGNED", assignedTo);
        entity.setAssignedTo(assignedTo);
        entity.setAssignedAt(LocalDateTime.now());
        entity.setStatus("ASSIGNED");
        log.info("隔离数据已分配: id={}, assignedTo={}", id, assignedTo);
        return quarantineRepository.save(entity);
    }

    @Transactional
    public CdrQuarantineDataEntity process(Long id, String action, String processedBy,
                                            String fixedData, String notes) {
        CdrQuarantineDataEntity entity = getById(id);
        String newStatus;
        switch (action) {
            case "FIX": newStatus = "FIXED"; break;
            case "DISCARD": newStatus = "DISCARDED"; break;
            case "RELEASE": newStatus = "RELEASED"; break;
            default: throw new BusinessException(400, "无效的处理操作: " + action);
        }
        quarantineRepository.processQuarantine(id, newStatus, processedBy, action, fixedData, notes);
        entity.setStatus(newStatus);
        entity.setProcessedBy(processedBy);
        entity.setProcessedAt(LocalDateTime.now());
        entity.setProcessAction(action);
        entity.setFixedData(fixedData);
        entity.setProcessNotes(notes);
        log.info("隔离数据已处理: id={}, action={}, by={}", id, action, processedBy);
        return quarantineRepository.save(entity);
    }

    @Transactional
    public void batchAssign(List<Long> ids, String assignedTo) {
        for (Long id : ids) {
            assign(id, assignedTo);
        }
    }

    public Map<String, Object> getStats() {
        long pending = quarantineRepository.countPendingQuarantines();
        long processing = quarantineRepository.countProcessingQuarantines();
        long fixed = quarantineRepository.countFixedQuarantines();
        long total = quarantineRepository.count();
        return Map.of(
            "total", total,
            "pending", pending,
            "processing", processing,
            "fixed", fixed,
            "others", total - pending - processing - fixed
        );
    }

    public List<CdrQuarantineDataEntity> getBySourceTable(String schema, String table) {
        return quarantineRepository.findBySourceSchemaAndSourceTableAndIsDeletedFalse(schema, table);
    }
}