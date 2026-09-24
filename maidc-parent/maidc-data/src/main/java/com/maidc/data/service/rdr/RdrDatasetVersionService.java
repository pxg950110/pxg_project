package com.maidc.data.service.rdr;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.rdr.RdrDatasetVersionEntity;
import com.maidc.data.repository.rdr.RdrDatasetVersionRepository;
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
 * RDR数据集版本服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RdrDatasetVersionService {

    private final RdrDatasetVersionRepository versionRepository;

    public Page<RdrDatasetVersionEntity> list(Long datasetId, String status, int page, int size) {
        Specification<RdrDatasetVersionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (datasetId != null) {
                predicates.add(cb.equal(root.get("datasetId"), datasetId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return versionRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public RdrDatasetVersionEntity getById(Long id) {
        return versionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "数据集版本不存在: " + id));
    }

    public RdrDatasetVersionEntity getByCode(String versionCode) {
        return versionRepository.findByVersionCodeAndIsDeletedFalse(versionCode)
                .orElseThrow(() -> new BusinessException(404, "数据集版本不存在: " + versionCode));
    }

    public List<RdrDatasetVersionEntity> getByDataset(Long datasetId) {
        return versionRepository.findByDatasetIdAndIsDeletedFalseOrderByCreatedAtDesc(datasetId);
    }

    public RdrDatasetVersionEntity getCurrentVersion(Long datasetId) {
        return versionRepository.findByDatasetIdAndIsCurrentAndIsDeletedFalse(datasetId, true).orElse(null);
    }

    public List<RdrDatasetVersionEntity> getDraftVersions() {
        return versionRepository.findDraftVersions();
    }

    @Transactional
    public RdrDatasetVersionEntity create(RdrDatasetVersionEntity entity) {
        if (entity.getVersionCode() == null || entity.getVersionCode().isBlank()) {
            entity.setVersionCode("VER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (versionRepository.existsByVersionCodeAndIsDeletedFalse(entity.getVersionCode())) {
            throw new BusinessException(400, "版本编码已存在: " + entity.getVersionCode());
        }
        if (entity.getVersionNumber() == null) entity.setVersionNumber("v1.0");
        if (entity.getStatus() == null) entity.setStatus("DRAFT");
        if (entity.getIsCurrent() == null) entity.setIsCurrent(false);
        if (entity.getTableCount() == null) entity.setTableCount(0);
        if (entity.getPatientCount() == null) entity.setPatientCount(0L);
        if (entity.getRecordCount() == null) entity.setRecordCount(0L);
        if (entity.getAccessCount() == null) entity.setAccessCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setOrgId(0L);
        RdrDatasetVersionEntity saved = versionRepository.save(entity);
        log.info("数据集版本创建: id={}, code={}", saved.getId(), saved.getVersionCode());
        return saved;
    }

    @Transactional
    public RdrDatasetVersionEntity publish(Long id, String publishedBy) {
        RdrDatasetVersionEntity entity = getById(id);
        if (!entity.getStatus().equals("DRAFT")) {
            throw new BusinessException(400, "只有DRAFT状态的版本可以发布");
        }
        // 清除当前版本标记
        versionRepository.clearCurrentFlag(entity.getDatasetId());
        // 设置新版本为当前版本
        versionRepository.setCurrentFlag(id);
        entity.setStatus("PUBLISHED");
        entity.setIsCurrent(true);
        entity.setPublishedAt(LocalDateTime.now());
        entity.setPublishedBy(publishedBy);
        log.info("数据集版本发布: id={}, datasetId={}", id, entity.getDatasetId());
        return versionRepository.save(entity);
    }

    @Transactional
    public RdrDatasetVersionEntity archive(Long id) {
        RdrDatasetVersionEntity entity = getById(id);
        entity.setStatus("ARCHIVED");
        entity.setIsCurrent(false);
        log.info("数据集版本归档: id={}", id);
        return versionRepository.save(entity);
    }

    @Transactional
    public RdrDatasetVersionEntity deprecate(Long id) {
        RdrDatasetVersionEntity entity = getById(id);
        entity.setStatus("DEPRECATED");
        entity.setIsCurrent(false);
        log.info("数据集版本废弃: id={}", id);
        return versionRepository.save(entity);
    }

    @Transactional
    public void recordAccess(Long versionId) {
        versionRepository.incrementAccessCount(versionId);
    }

    public Map<String, Object> getStats() {
        long published = versionRepository.countPublishedVersions();
        long total = versionRepository.count();
        return Map.of("total", total, "published", published, "draft", total - published);
    }
}