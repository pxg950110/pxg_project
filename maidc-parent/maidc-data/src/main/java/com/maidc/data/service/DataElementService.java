package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.dto.DataElementCreateDTO;
import com.maidc.data.dto.DataElementMappingDTO;
import com.maidc.data.entity.DataElementEntity;
import com.maidc.data.entity.DataElementMappingEntity;
import com.maidc.data.entity.DataElementValueEntity;
import com.maidc.data.repository.DataElementMappingRepository;
import com.maidc.data.repository.DataElementRepository;
import com.maidc.data.repository.DataElementValueRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class DataElementService {

    private final DataElementRepository dataElementRepository;
    private final DataElementValueRepository valueRepository;
    private final DataElementMappingRepository mappingRepository;

    public Page<DataElementEntity> list(String category, String registrationStatus, String keyword,
                                         String dataType, int page, int size) {
        Specification<DataElementEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (registrationStatus != null && !registrationStatus.isBlank()) {
                predicates.add(cb.equal(root.get("registrationStatus"), registrationStatus));
            }
            if (dataType != null && !dataType.isBlank()) {
                predicates.add(cb.equal(root.get("dataType"), dataType));
            }
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.or(
                        cb.like(root.get("name"), "%" + keyword + "%"),
                        cb.like(root.get("nameEn"), "%" + keyword + "%"),
                        cb.like(root.get("elementCode"), "%" + keyword + "%"),
                        cb.like(root.get("definition"), "%" + keyword + "%")
                ));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return dataElementRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public DataElementEntity getById(Long id) {
        return dataElementRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "数据元不存在: " + id));
    }

    @Transactional
    public DataElementEntity create(DataElementCreateDTO dto) {
        if (dataElementRepository.existsByElementCodeAndIsDeletedFalse(dto.getElementCode())) {
            throw new BusinessException(400, "标识符已存在: " + dto.getElementCode());
        }
        DataElementEntity entity = new DataElementEntity();
        entity.setElementCode(dto.getElementCode());
        entity.setName(dto.getName());
        entity.setNameEn(dto.getNameEn());
        entity.setDefinition(dto.getDefinition());
        entity.setObjectClassName(dto.getObjectClassName());
        entity.setObjectClassId(dto.getObjectClassId());
        entity.setPropertyName(dto.getPropertyName());
        entity.setPropertyId(dto.getPropertyId());
        entity.setDataType(dto.getDataType());
        entity.setRepresentationClass(dto.getRepresentationClass());
        entity.setValueDomainName(dto.getValueDomainName());
        entity.setValueDomainId(dto.getValueDomainId());
        entity.setMinLength(dto.getMinLength());
        entity.setMaxLength(dto.getMaxLength());
        entity.setFormat(dto.getFormat());
        entity.setUnitOfMeasure(dto.getUnitOfMeasure());
        entity.setCategory(dto.getCategory());
        entity.setStandardSource(dto.getStandardSource());
        entity.setRegistrationStatus(dto.getRegistrationStatus() != null ? dto.getRegistrationStatus() : "DRAFT");
        entity.setVersion(dto.getVersion() != null ? dto.getVersion() : "1.0");
        entity.setSynonyms(dto.getSynonyms());
        entity.setKeywords(dto.getKeywords());
        entity.setExtraAttrs(dto.getExtraAttrs());
        entity.setOrgId(0L);
        DataElementEntity saved = dataElementRepository.save(entity);
        log.info("数据元创建: id={}, code={}", saved.getId(), saved.getElementCode());
        return saved;
    }

    @Transactional
    public DataElementEntity update(Long id, DataElementCreateDTO dto) {
        DataElementEntity entity = getById(id);
        if (dto.getElementCode() != null) entity.setElementCode(dto.getElementCode());
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getNameEn() != null) entity.setNameEn(dto.getNameEn());
        if (dto.getDefinition() != null) entity.setDefinition(dto.getDefinition());
        if (dto.getObjectClassName() != null) entity.setObjectClassName(dto.getObjectClassName());
        if (dto.getObjectClassId() != null) entity.setObjectClassId(dto.getObjectClassId());
        if (dto.getPropertyName() != null) entity.setPropertyName(dto.getPropertyName());
        if (dto.getPropertyId() != null) entity.setPropertyId(dto.getPropertyId());
        if (dto.getDataType() != null) entity.setDataType(dto.getDataType());
        if (dto.getRepresentationClass() != null) entity.setRepresentationClass(dto.getRepresentationClass());
        if (dto.getValueDomainName() != null) entity.setValueDomainName(dto.getValueDomainName());
        if (dto.getValueDomainId() != null) entity.setValueDomainId(dto.getValueDomainId());
        if (dto.getMinLength() != null) entity.setMinLength(dto.getMinLength());
        if (dto.getMaxLength() != null) entity.setMaxLength(dto.getMaxLength());
        if (dto.getFormat() != null) entity.setFormat(dto.getFormat());
        if (dto.getUnitOfMeasure() != null) entity.setUnitOfMeasure(dto.getUnitOfMeasure());
        if (dto.getCategory() != null) entity.setCategory(dto.getCategory());
        if (dto.getStandardSource() != null) entity.setStandardSource(dto.getStandardSource());
        if (dto.getRegistrationStatus() != null) entity.setRegistrationStatus(dto.getRegistrationStatus());
        if (dto.getVersion() != null) entity.setVersion(dto.getVersion());
        if (dto.getSynonyms() != null) entity.setSynonyms(dto.getSynonyms());
        if (dto.getKeywords() != null) entity.setKeywords(dto.getKeywords());
        if (dto.getExtraAttrs() != null) entity.setExtraAttrs(dto.getExtraAttrs());
        return dataElementRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        DataElementEntity entity = getById(id);
        entity.setStatus("RETIRED");
        dataElementRepository.save(entity);
        dataElementRepository.delete(entity);
        log.info("数据元已删除: id={}, code={}", id, entity.getElementCode());
    }

    // ── 允许值 ──

    public List<DataElementValueEntity> getValues(Long dataElementId) {
        return valueRepository.findByDataElementIdAndIsDeletedFalseOrderBySortOrder(dataElementId);
    }

    @Transactional
    public List<DataElementValueEntity> updateValues(Long dataElementId, List<DataElementValueEntity> values) {
        valueRepository.deleteByDataElementId(dataElementId);
        List<DataElementValueEntity> saved = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            DataElementValueEntity v = values.get(i);
            v.setId(null);
            v.setDataElementId(dataElementId);
            v.setSortOrder(i);
            v.setOrgId(0L);
            saved.add(valueRepository.save(v));
        }
        return saved;
    }

    // ── 映射 ──

    public List<DataElementMappingEntity> getMappings(Long dataElementId) {
        return mappingRepository.findByDataElementIdAndIsDeletedFalse(dataElementId);
    }

    @Transactional
    public DataElementMappingEntity addMapping(Long dataElementId, DataElementMappingDTO dto) {
        DataElementMappingEntity entity = new DataElementMappingEntity();
        entity.setDataElementId(dataElementId);
        entity.setSchemaName(dto.getSchemaName());
        entity.setTableName(dto.getTableName());
        entity.setColumnName(dto.getColumnName());
        entity.setMappingType(dto.getMappingType() != null ? dto.getMappingType() : "MANUAL");
        entity.setConfidence(dto.getConfidence());
        entity.setMappingStatus(dto.getMappingStatus() != null ? dto.getMappingStatus() : "PENDING");
        entity.setTransformRule(dto.getTransformRule());
        entity.setMappedBy("system");
        entity.setMappedAt(LocalDateTime.now());
        entity.setOrgId(0L);
        return mappingRepository.save(entity);
    }

    @Transactional
    public DataElementMappingEntity updateMapping(Long mappingId, String mappingStatus) {
        DataElementMappingEntity entity = mappingRepository.findByIdAndIsDeletedFalse(mappingId)
                .orElseThrow(() -> new BusinessException(404, "映射不存在: " + mappingId));
        entity.setMappingStatus(mappingStatus);
        entity.setMappedAt(LocalDateTime.now());
        return mappingRepository.save(entity);
    }

    @Transactional
    public void deleteMapping(Long mappingId) {
        mappingRepository.deleteById(mappingId);
    }

    // ── 分类与统计 ──

    public List<String> getCategories() {
        return dataElementRepository.findDistinctCategories();
    }

    public Map<String, Object> getStats() {
        long total = dataElementRepository.count();
        long draft = dataElementRepository.findAll().stream().filter(e -> "DRAFT".equals(e.getRegistrationStatus())).count();
        long published = dataElementRepository.findAll().stream().filter(e -> "PUBLISHED".equals(e.getRegistrationStatus())).count();
        return Map.of("total", total, "draft", draft, "published", published);
    }
}
