package com.maidc.data.service.rdr;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.rdr.RdrMultimodalLinkEntity;
import com.maidc.data.entity.rdr.RdrMultimodalLinkGroupEntity;
import com.maidc.data.entity.rdr.RdrCompletenessAssessmentEntity;
import com.maidc.data.repository.rdr.RdrMultimodalLinkRepository;
import com.maidc.data.repository.rdr.RdrMultimodalLinkGroupRepository;
import com.maidc.data.repository.rdr.RdrCompletenessAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RDR多模态数据服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RdrMultimodalService {

    private final RdrMultimodalLinkRepository linkRepository;
    private final RdrMultimodalLinkGroupRepository groupRepository;
    private final RdrCompletenessAssessmentRepository completenessRepository;

    // ── 多模态关联 ──

    public Page<RdrMultimodalLinkEntity> listLinks(Long patientId, Long encounterId,
                                                      String modalityType, String status, int page, int size) {
        Specification<RdrMultimodalLinkEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (patientId != null) {
                predicates.add(cb.equal(root.get("patientId"), patientId));
            }
            if (encounterId != null) {
                predicates.add(cb.equal(root.get("encounterId"), encounterId));
            }
            if (modalityType != null && !modalityType.isBlank()) {
                predicates.add(cb.equal(root.get("modalityType"), modalityType));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return linkRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "timestamp")));
    }

    public RdrMultimodalLinkEntity getLinkById(Long id) {
        return linkRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "多模态关联不存在: " + id));
    }

    public List<RdrMultimodalLinkEntity> getLinksByPatient(Long patientId) {
        return linkRepository.findByPatientIdAndIsDeletedFalse(patientId);
    }

    public List<RdrMultimodalLinkEntity> getLinksByTimeRange(Long patientId, LocalDateTime start, LocalDateTime end) {
        return linkRepository.findByPatientAndTimeRange(patientId, start, end);
    }

    public List<Object[]> getModalityCountForPatient(Long patientId) {
        return linkRepository.countByModalityTypeForPatient(patientId);
    }

    @Transactional
    public RdrMultimodalLinkEntity createLink(RdrMultimodalLinkEntity entity) {
        if (entity.getLinkType() == null) entity.setLinkType("TEMPORAL");
        if (entity.getLinkSource() == null) entity.setLinkSource("AUTO_TEMPORAL");
        if (entity.getStatus() == null) entity.setStatus("ACTIVE");
        if (entity.getIsValidated() == null) entity.setIsValidated(false);
        entity.setOrgId(0L);
        RdrMultimodalLinkEntity saved = linkRepository.save(entity);
        log.info("多模态关联创建: id={}, patientId={}, modality={}", saved.getId(), saved.getPatientId(), saved.getModalityType());
        return saved;
    }

    @Transactional
    public RdrMultimodalLinkEntity validateLink(Long id, String validatedBy) {
        RdrMultimodalLinkEntity entity = getLinkById(id);
        linkRepository.validateLink(id, validatedBy);
        entity.setIsValidated(true);
        entity.setValidatedBy(validatedBy);
        entity.setValidatedAt(LocalDateTime.now());
        log.info("多模态关联验证: id={}, by={}", id, validatedBy);
        return linkRepository.save(entity);
    }

    @Transactional
    public void deactivateLink(Long id) {
        RdrMultimodalLinkEntity entity = getLinkById(id);
        entity.setStatus("INACTIVE");
        linkRepository.save(entity);
    }

    // ── 关联组 ──

    public RdrMultimodalLinkGroupEntity getGroupById(Long id) {
        return groupRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "关联组不存在: " + id));
    }

    public List<RdrMultimodalLinkGroupEntity> getGroupsByPatient(Long patientId) {
        return groupRepository.findByPatientIdAndIsDeletedFalse(patientId);
    }

    public List<RdrMultimodalLinkGroupEntity> getActiveGroupsForPatient(Long patientId) {
        return groupRepository.findActiveGroupsForPatient(patientId);
    }

    @Transactional
    public RdrMultimodalLinkGroupEntity createGroup(RdrMultimodalLinkGroupEntity entity) {
        if (entity.getGroupCode() == null || entity.getGroupCode().isBlank()) {
            entity.setGroupCode("GRP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (groupRepository.existsByGroupCodeAndIsDeletedFalse(entity.getGroupCode())) {
            throw new BusinessException(400, "关联组编码已存在: " + entity.getGroupCode());
        }
        if (entity.getGroupType() == null) entity.setGroupType("ENCOUNTER_BASED");
        if (entity.getStatus() == null) entity.setStatus("ACTIVE");
        if (entity.getClinicalCount() == null) entity.setClinicalCount(0);
        if (entity.getImagingCount() == null) entity.setImagingCount(0);
        if (entity.getGenomicCount() == null) entity.setGenomicCount(0);
        if (entity.getTextCount() == null) entity.setTextCount(0);
        if (entity.getWaveformCount() == null) entity.setWaveformCount(0);
        if (entity.getPathologyCount() == null) entity.setPathologyCount(0);
        if (entity.getTotalLinkCount() == null) entity.setTotalLinkCount(0);
        entity.setOrgId(0L);
        RdrMultimodalLinkGroupEntity saved = groupRepository.save(entity);
        log.info("关联组创建: id={}, patientId={}", saved.getId(), saved.getPatientId());
        return saved;
    }

    @Transactional
    public RdrMultimodalLinkGroupEntity updateGroupCounts(Long groupId, String modalityType, int delta) {
        RdrMultimodalLinkGroupEntity group = getGroupById(groupId);
        switch (modalityType) {
            case "CLINICAL": group.setClinicalCount(group.getClinicalCount() + delta); break;
            case "IMAGING": group.setImagingCount(group.getImagingCount() + delta); break;
            case "GENOMIC": group.setGenomicCount(group.getGenomicCount() + delta); break;
            case "TEXT": group.setTextCount(group.getTextCount() + delta); break;
            case "WAVEFORM": group.setWaveformCount(group.getWaveformCount() + delta); break;
            case "PATHOLOGY": group.setPathologyCount(group.getPathologyCount() + delta); break;
        }
        group.setTotalLinkCount(group.getTotalLinkCount() + delta);
        return groupRepository.save(group);
    }

    // ── 完整性评估 ──

    public RdrCompletenessAssessmentEntity getAssessmentById(Long id) {
        return completenessRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "完整性评估不存在: " + id));
    }

    public List<RdrCompletenessAssessmentEntity> getAssessmentsByPatient(Long patientId) {
        return completenessRepository.findByPatientIdAndIsDeletedFalseOrderByAssessmentTimeDesc(patientId);
    }

    public RdrCompletenessAssessmentEntity getLatestAssessment(Long patientId) {
        return completenessRepository.findLatestCompletedAssessment(patientId).orElse(null);
    }

    @Transactional
    public RdrCompletenessAssessmentEntity createAssessment(RdrCompletenessAssessmentEntity entity) {
        if (entity.getAssessmentCode() == null || entity.getAssessmentCode().isBlank()) {
            entity.setAssessmentCode("ASM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (completenessRepository.existsByAssessmentCodeAndIsDeletedFalse(entity.getAssessmentCode())) {
            throw new BusinessException(400, "评估编码已存在: " + entity.getAssessmentCode());
        }
        if (entity.getAssessmentTime() == null) entity.setAssessmentTime(LocalDateTime.now());
        if (entity.getStatus() == null) entity.setStatus("COMPLETED");
        if (entity.getAssessedBy() == null) entity.setAssessedBy("AUTO_SYSTEM");
        if (entity.getDataElementsExpected() == null) entity.setDataElementsExpected(0);
        if (entity.getDataElementsPresent() == null) entity.setDataElementsPresent(0);
        if (entity.getCriticalElementsMissing() == null) entity.setCriticalElementsMissing(0);
        entity.setOrgId(0L);
        RdrCompletenessAssessmentEntity saved = completenessRepository.save(entity);
        log.info("完整性评估创建: id={}, patientId={}, overallScore={}", saved.getId(), saved.getPatientId(), saved.getOverallScore());
        return saved;
    }

    public Map<String, Object> getCompletenessStats(Long patientId) {
        RdrCompletenessAssessmentEntity latest = getLatestAssessment(patientId);
        if (latest == null) {
            return Map.of("hasAssessment", false);
        }
        return Map.of(
            "hasAssessment", true,
            "overallScore", latest.getOverallScore(),
            "completenessLevel", latest.getCompletenessLevel() != null ? latest.getCompletenessLevel() : "UNKNOWN",
            "clinicalScore", latest.getClinicalScore() != null ? latest.getClinicalScore() : 0,
            "imagingScore", latest.getImagingScore() != null ? latest.getImagingScore() : 0,
            "genomicScore", latest.getGenomicScore() != null ? latest.getGenomicScore() : 0,
            "textScore", latest.getTextScore() != null ? latest.getTextScore() : 0,
            "criticalMissing", latest.getCriticalElementsMissing() != null ? latest.getCriticalElementsMissing() : 0
        );
    }

    public Double getAverageCompletenessScore() {
        return completenessRepository.getAverageOverallScore();
    }

    public List<Object[]> getCompletenessDistribution() {
        return completenessRepository.countByCompletenessLevel();
    }
}