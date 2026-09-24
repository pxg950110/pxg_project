package com.maidc.data.service.rdr;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.rdr.RdrSelectionConfigEntity;
import com.maidc.data.entity.rdr.RdrSelectionResultEntity;
import com.maidc.data.entity.rdr.RdrCohortMemberSnapshotEntity;
import com.maidc.data.repository.rdr.RdrSelectionConfigRepository;
import com.maidc.data.repository.rdr.RdrSelectionResultRepository;
import com.maidc.data.repository.rdr.RdrCohortMemberSnapshotRepository;
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
 * RDR队列筛选服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RdrSelectionService {

    private final RdrSelectionConfigRepository configRepository;
    private final RdrSelectionResultRepository resultRepository;
    private final RdrCohortMemberSnapshotRepository memberRepository;

    // ── 筛选配置 ──

    public Page<RdrSelectionConfigEntity> listConfigs(Long cohortId, String status, int page, int size) {
        Specification<RdrSelectionConfigEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (cohortId != null) {
                predicates.add(cb.equal(root.get("cohortId"), cohortId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return configRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public RdrSelectionConfigEntity getConfigById(Long id) {
        return configRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "筛选配置不存在: " + id));
    }

    @Transactional
    public RdrSelectionConfigEntity createConfig(RdrSelectionConfigEntity entity) {
        if (entity.getSelectionCode() == null || entity.getSelectionCode().isBlank()) {
            entity.setSelectionCode("SEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (configRepository.existsBySelectionCodeAndIsDeletedFalse(entity.getSelectionCode())) {
            throw new BusinessException(400, "筛选编码已存在: " + entity.getSelectionCode());
        }
        if (entity.getDataSource() == null) entity.setDataSource("CDR");
        if (entity.getEncounterType() == null) entity.setEncounterType("ALL");
        if (entity.getRuleLogic() == null) entity.setRuleLogic("AND");
        if (entity.getStatus() == null) entity.setStatus("DRAFT");
        if (entity.getExecutionCount() == null) entity.setExecutionCount(0);
        entity.setOrgId(0L);
        RdrSelectionConfigEntity saved = configRepository.save(entity);
        log.info("筛选配置创建: id={}, code={}", saved.getId(), saved.getSelectionCode());
        return saved;
    }

    @Transactional
    public RdrSelectionConfigEntity updateConfig(Long id, RdrSelectionConfigEntity updates) {
        RdrSelectionConfigEntity entity = getConfigById(id);
        if (updates.getInclusionRules() != null) entity.setInclusionRules(updates.getInclusionRules());
        if (updates.getExclusionRules() != null) entity.setExclusionRules(updates.getExclusionRules());
        if (updates.getRuleLogic() != null) entity.setRuleLogic(updates.getRuleLogic());
        if (updates.getTimeRangeStart() != null) entity.setTimeRangeStart(updates.getTimeRangeStart());
        if (updates.getTimeRangeEnd() != null) entity.setTimeRangeEnd(updates.getTimeRangeEnd());
        if (updates.getEncounterType() != null) entity.setEncounterType(updates.getEncounterType());
        if (updates.getMinEncounters() != null) entity.setMinEncounters(updates.getMinEncounters());
        if (updates.getDataCompletenessThreshold() != null) entity.setDataCompletenessThreshold(updates.getDataCompletenessThreshold());
        if (updates.getDescription() != null) entity.setDescription(updates.getDescription());
        return configRepository.save(entity);
    }

    @Transactional
    public RdrSelectionConfigEntity activateConfig(Long id) {
        RdrSelectionConfigEntity entity = getConfigById(id);
        entity.setStatus("ACTIVE");
        log.info("筛选配置激活: id={}", id);
        return configRepository.save(entity);
    }

    // ── 筛选执行 ──

    public Page<RdrSelectionResultEntity> listResults(Long selectionId, Long cohortId, String status, int page, int size) {
        Specification<RdrSelectionResultEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (selectionId != null) {
                predicates.add(cb.equal(root.get("selectionId"), selectionId));
            }
            if (cohortId != null) {
                predicates.add(cb.equal(root.get("cohortId"), cohortId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return resultRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "executionTime")));
    }

    public RdrSelectionResultEntity getResultById(Long id) {
        return resultRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "筛选结果不存在: " + id));
    }

    public RdrSelectionResultEntity getLatestCompletedResult(Long cohortId) {
        return resultRepository.findLatestCompletedResult(cohortId).orElse(null);
    }

    @Transactional
    public RdrSelectionResultEntity startSelection(Long selectionId, String triggeredBy) {
        RdrSelectionConfigEntity config = getConfigById(selectionId);
        if (!config.getStatus().equals("ACTIVE")) {
            throw new BusinessException(400, "筛选配置未激活");
        }

        RdrSelectionResultEntity result = new RdrSelectionResultEntity();
        result.setResultCode("RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.setSelectionId(selectionId);
        result.setCohortId(config.getCohortId());
        result.setExecutionTime(LocalDateTime.now());
        result.setSourcePatientCount(0L);
        result.setPassedInclusionCount(0L);
        result.setPassedExclusionCount(0L);
        result.setFinalSelectedCount(0L);
        result.setExcludedCount(0L);
        result.setStatus("RUNNING");
        result.setTriggeredBy(triggeredBy != null ? triggeredBy : "MANUAL");
        result.setIsSnapshotCreated(false);
        result.setOrgId(0L);

        configRepository.incrementExecutionCount(selectionId);
        return resultRepository.save(result);
    }

    @Transactional
    public void updateResultProgress(Long resultId, Long sourceCount, Long inclusionCount,
                                      Long exclusionCount, Long finalCount) {
        RdrSelectionResultEntity result = getResultById(resultId);
        result.setSourcePatientCount(sourceCount);
        result.setPassedInclusionCount(inclusionCount);
        result.setPassedExclusionCount(exclusionCount);
        result.setFinalSelectedCount(finalCount);
        result.setExcludedCount(sourceCount - finalCount);
        resultRepository.save(result);
    }

    @Transactional
    public void completeSelection(Long resultId, String ruleHitStats, String exclusionStats) {
        RdrSelectionResultEntity result = getResultById(resultId);
        result.setStatus("COMPLETED");
        result.setRuleHitStats(ruleHitStats);
        result.setExclusionReasonStats(exclusionStats);
        resultRepository.save(result);
        log.info("筛选完成: resultId={}, finalSelected={}", resultId, result.getFinalSelectedCount());
    }

    // ── 成员快照 ──

    public List<RdrCohortMemberSnapshotEntity> getMembersByResult(Long resultId) {
        return memberRepository.findByResultIdAndIsActiveAndIsDeletedFalse(resultId, true);
    }

    public List<RdrCohortMemberSnapshotEntity> getMembersByCohort(Long cohortId) {
        return memberRepository.findByCohortIdAndIsActiveAndIsDeletedFalse(cohortId, true);
    }

    public long countActiveMembers(Long resultId) {
        return memberRepository.countActiveMembersByResult(resultId);
    }

    @Transactional
    public RdrCohortMemberSnapshotEntity addMember(Long resultId, Long cohortId, Long patientId,
                                                    String patientNo, BigDecimal matchScore,
                                                    BigDecimal completeness, Integer encounterCount) {
        RdrCohortMemberSnapshotEntity member = new RdrCohortMemberSnapshotEntity();
        member.setResultId(resultId);
        member.setCohortId(cohortId);
        member.setPatientId(patientId);
        member.setPatientNo(patientNo);
        member.setMatchScore(matchScore);
        member.setDataCompleteness(completeness);
        member.setEncounterCount(encounterCount != null ? encounterCount : 0);
        member.setDiagnosisCount(0);
        member.setLabCount(0);
        member.setIsActive(true);
        member.setOrgId(0L);
        return memberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long resultId, Long patientId, String reason) {
        memberRepository.removeMember(resultId, reason, patientId);
        log.info("成员移出队列: resultId={}, patientId={}, reason={}", resultId, patientId, reason);
    }

    public Map<String, Object> getStats(Long resultId) {
        long total = memberRepository.countActiveMembersByResult(resultId);
        Double avgCompleteness = memberRepository.getAverageCompleteness(resultId);
        return Map.of(
            "totalMembers", total,
            "averageCompleteness", avgCompleteness != null ? avgCompleteness : 0
        );
    }
}