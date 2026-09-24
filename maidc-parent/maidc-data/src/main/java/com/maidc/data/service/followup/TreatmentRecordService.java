package com.maidc.data.service.followup;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.MedicationEntity;
import com.maidc.data.entity.PatientFollowupEntity;
import com.maidc.data.entity.TreatmentRecordEntity;
import com.maidc.data.repository.MedicationRepository;
import com.maidc.data.repository.TreatmentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 治疗记录：手工 CRUD + CDR 候选枚举与带入（只读快照，重复自动跳过并列出）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TreatmentRecordService {

    private final TreatmentRecordRepository treatmentRepository;
    private final PatientFollowupService followupService;
    private final MedicationRepository medicationRepository;

    public List<TreatmentRecordEntity> list(Long followupId) {
        return treatmentRepository.findByFollowupIdAndIsDeletedFalseOrderByOccurredDateDesc(followupId);
    }

    @Transactional
    public TreatmentRecordEntity create(Long followupId, Map<String, Object> req) {
        PatientFollowupEntity followup = followupService.get(followupId);
        TreatmentRecordEntity entity = new TreatmentRecordEntity();
        entity.setFollowupId(followup.getId());
        entity.setTaskId(req.get("taskId") == null ? null : Long.parseLong(String.valueOf(req.get("taskId"))));
        entity.setCategory(String.valueOf(req.getOrDefault("category", "OTHER")));
        entity.setName(String.valueOf(req.get("name")));
        if (entity.getName().isBlank()) throw new BusinessException(400, "治疗名称必填");
        entity.setOccurredDate(req.get("occurredDate") == null ? LocalDate.now()
                : LocalDate.parse(String.valueOf(req.get("occurredDate"))));
        entity.setSource("MANUAL");
        if (req.get("detail") != null) {
            try { entity.setDetail(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(req.get("detail"))); }
            catch (Exception ignored) { }
        }
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        return treatmentRepository.save(entity);
    }

    /** CDR 候选：该患者用药记录（最近 180 天），标注与档案内已有 CDR 快照重复项 */
    public List<Map<String, Object>> cdrCandidates(Long followupId) {
        PatientFollowupEntity followup = followupService.get(followupId);
        LocalDateTime since = LocalDateTime.now().minusDays(180);
        List<MedicationEntity> meds = medicationRepository.findByPatientIdAndIsDeletedFalse(followup.getPatientId()).stream()
                .filter(m -> m.getStartTime() != null && m.getStartTime().isAfter(since))
                .toList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (MedicationEntity m : meds) {
            LocalDate occurred = m.getStartTime().toLocalDate();
            boolean exists = treatmentRepository.existsCdrDuplicate(followupId, medName(m), occurred);
            Map<String, Object> item = new HashMap<>();
            item.put("resourceType", "MedicationRequest");
            item.put("recordId", m.getId());
            item.put("name", medName(m));
            item.put("occurredDate", occurred.toString());
            item.put("category", "MEDICATION");
            item.put("detail", Map.of("dosage", m.getDosage() == null ? "" : m.getDosage(),
                    "route", m.getRoute() == null ? "" : m.getRoute()));
            item.put("exists", exists);
            result.add(item);
        }
        return result;
    }

    /** 勾选带入：source=CDR 只读快照；同 name+occurred_date 已存在则跳过并列出 */
    @Transactional
    public Map<String, Object> importCdr(Long followupId, List<Map<String, Object>> items) {
        PatientFollowupEntity followup = followupService.get(followupId);
        int imported = 0;
        List<Map<String, Object>> skipped = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String name = String.valueOf(item.get("name"));
            LocalDate occurred = LocalDate.parse(String.valueOf(item.get("occurredDate")));
            if (treatmentRepository.existsCdrDuplicate(followupId, name, occurred)) {
                skipped.add(item);
                continue;
            }
            TreatmentRecordEntity entity = new TreatmentRecordEntity();
            entity.setFollowupId(followupId);
            entity.setCategory(String.valueOf(item.getOrDefault("category", "MEDICATION")));
            entity.setName(name);
            entity.setOccurredDate(occurred);
            entity.setSource("CDR");
            if (item.get("detail") != null) {
                try { entity.setDetail(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(item.get("detail"))); }
                catch (Exception ignored) { }
            }
            Map<String, Object> ref = new HashMap<>();
            ref.put("resourceType", item.get("resourceType"));
            ref.put("recordId", item.get("recordId"));
            try { entity.setSourceRef(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(ref)); }
            catch (Exception ignored) { }
            if (entity.getOrgId() == null) entity.setOrgId(0L);
            treatmentRepository.save(entity);
            imported++;
        }
        log.info("CDR 治疗带入: followup={} 导入 {} 跳过 {}", followupId, imported, skipped.size());
        return Map.of("imported", imported, "skipped", skipped);
    }

    private String medName(MedicationEntity m) {
        return m.getMedName() != null ? m.getMedName() : ("药物#" + m.getId());
    }
}
