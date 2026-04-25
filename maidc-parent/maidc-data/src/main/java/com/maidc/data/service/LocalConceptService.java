package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.ConceptEntity;
import com.maidc.data.entity.LocalConceptEntity;
import com.maidc.data.repository.ConceptRepository;
import com.maidc.data.repository.LocalConceptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalConceptService {

    private final LocalConceptRepository localConceptRepository;
    private final ConceptRepository conceptRepository;

    public Page<LocalConceptEntity> list(Long institutionId, Long codeSystemId, String mappingStatus, int page, int size) {
        if (mappingStatus != null && !mappingStatus.isBlank()) {
            return localConceptRepository.findByInstitutionIdAndCodeSystemIdAndMappingStatusAndIsDeletedFalse(
                    institutionId, codeSystemId, mappingStatus, PageRequest.of(page - 1, size));
        }
        return localConceptRepository.findByInstitutionIdAndCodeSystemIdAndIsDeletedFalse(
                institutionId, codeSystemId, PageRequest.of(page - 1, size));
    }

    public Page<LocalConceptEntity> getUnmapped(Long institutionId, Long codeSystemId, int page, int size) {
        return localConceptRepository.findUnmapped(institutionId, codeSystemId, PageRequest.of(page - 1, size));
    }

    public Map<String, Object> getStats(Long institutionId, Long codeSystemId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("institutionId", institutionId);
        stats.put("codeSystemId", codeSystemId);
        stats.put("CONFIRMED", localConceptRepository.countByStatus(institutionId, codeSystemId, "CONFIRMED"));
        stats.put("AUTO", localConceptRepository.countByStatus(institutionId, codeSystemId, "AUTO"));
        stats.put("SUSPECTED", localConceptRepository.countByStatus(institutionId, codeSystemId, "SUSPECTED"));
        stats.put("UNMAPPED", localConceptRepository.countByStatus(institutionId, codeSystemId, "UNMAPPED"));
        return stats;
    }

    public LocalConceptEntity getById(Long id) {
        return localConceptRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "本地编码不存在: " + id));
    }

    @Transactional
    public LocalConceptEntity create(LocalConceptEntity entity) {
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        LocalConceptEntity saved = localConceptRepository.save(entity);
        log.info("本地编码创建成功: id={}, localCode={}", saved.getId(), saved.getLocalCode());
        return saved;
    }

    @Transactional
    public List<LocalConceptEntity> batchCreate(List<LocalConceptEntity> entities) {
        for (LocalConceptEntity entity : entities) {
            if (entity.getOrgId() == null) entity.setOrgId(0L);
        }
        List<LocalConceptEntity> saved = localConceptRepository.saveAll(entities);
        log.info("批量创建本地编码: count={}", saved.size());
        return saved;
    }

    @Transactional
    public LocalConceptEntity update(Long id, LocalConceptEntity updates) {
        LocalConceptEntity entity = getById(id);
        if (updates.getLocalName() != null) entity.setLocalName(updates.getLocalName());
        if (updates.getStandardConceptId() != null) entity.setStandardConceptId(updates.getStandardConceptId());
        if (updates.getMappingConfidence() != null) entity.setMappingConfidence(updates.getMappingConfidence());
        if (updates.getMappingStatus() != null) entity.setMappingStatus(updates.getMappingStatus());
        if (updates.getMappedBy() != null) entity.setMappedBy(updates.getMappedBy());
        entity.setMappedAt(LocalDateTime.now());
        return localConceptRepository.save(entity);
    }

    public Map<String, Object> translateById(Long institutionId, Long codeSystemId, String localCode) {
        LocalConceptEntity localConcept = localConceptRepository
                .findByInstitutionIdAndCodeSystemIdAndLocalCodeAndIsDeletedFalse(institutionId, codeSystemId, localCode)
                .orElseThrow(() -> new BusinessException(404, "本地编码不存在: " + localCode));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("localCode", localConcept.getLocalCode());
        result.put("localName", localConcept.getLocalName());
        result.put("mappingStatus", localConcept.getMappingStatus());
        result.put("mappingConfidence", localConcept.getMappingConfidence());

        if (localConcept.getStandardConceptId() != null) {
            ConceptEntity standard = conceptRepository.findById(localConcept.getStandardConceptId())
                    .orElse(null);
            if (standard != null) {
                Map<String, Object> stdInfo = new LinkedHashMap<>();
                stdInfo.put("id", standard.getId());
                stdInfo.put("conceptCode", standard.getConceptCode());
                stdInfo.put("name", standard.getName());
                stdInfo.put("nameEn", standard.getNameEn());
                result.put("standardConcept", stdInfo);
            }
        }
        return result;
    }
}
