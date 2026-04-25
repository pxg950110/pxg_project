package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.CodeSystemEntity;
import com.maidc.data.repository.CodeSystemRepository;
import com.maidc.data.repository.ConceptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeSystemService {

    private final CodeSystemRepository codeSystemRepository;
    private final ConceptRepository conceptRepository;

    public List<CodeSystemEntity> listAll() {
        return codeSystemRepository.findAll();
    }

    public CodeSystemEntity getById(Long id) {
        return codeSystemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "编码体系不存在: " + id));
    }

    public Map<String, Object> getStats(Long id) {
        CodeSystemEntity codeSystem = getById(id);
        long conceptCount = conceptRepository.findByCodeSystemIdAndIsDeletedFalse(id, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("codeSystem", codeSystem);
        stats.put("conceptCount", conceptCount);
        return stats;
    }

    @Transactional
    public CodeSystemEntity create(CodeSystemEntity entity) {
        if (codeSystemRepository.existsByCodeAndIsDeletedFalse(entity.getCode())) {
            throw new BusinessException(400, "编码体系代码已存在: " + entity.getCode());
        }
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        CodeSystemEntity saved = codeSystemRepository.save(entity);
        log.info("编码体系创建成功: id={}, code={}", saved.getId(), saved.getCode());
        return saved;
    }

    @Transactional
    public CodeSystemEntity update(Long id, CodeSystemEntity updates) {
        CodeSystemEntity entity = getById(id);
        if (updates.getName() != null) entity.setName(updates.getName());
        if (updates.getVersion() != null) entity.setVersion(updates.getVersion());
        if (updates.getDescription() != null) entity.setDescription(updates.getDescription());
        if (updates.getHierarchySupport() != null) entity.setHierarchySupport(updates.getHierarchySupport());
        if (updates.getStatus() != null) entity.setStatus(updates.getStatus());
        return codeSystemRepository.save(entity);
    }
}
