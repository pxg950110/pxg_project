package com.maidc.data.service.followup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.FollowupProtocolEntity;
import com.maidc.data.entity.FollowupTaskEntity;
import com.maidc.data.entity.PatientFollowupEntity;
import com.maidc.data.repository.DiseaseCohortPatientRepository;
import com.maidc.data.repository.FollowupTaskRepository;
import com.maidc.data.repository.PatientFollowupRepository;
import com.maidc.data.repository.ScaleDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 患者随访档案：建档（快照方案版本 + 按阶段全量生成任务）与状态机。
 * followup: ACTIVE ⇄ SUSPENDED → CLOSED(原因必填) / → OUT_OF_COHORT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientFollowupService {

    private final PatientFollowupRepository followupRepository;
    private final FollowupTaskRepository taskRepository;
    private final FollowupProtocolService protocolService;
    private final DiseaseCohortPatientRepository cohortPatientRepository;
    private final ScaleDefinitionRepository scaleRepository;
    private final DeptScopeService deptScopeService;
    private final com.maidc.data.repository.EncounterRepository encounterRepository;
    private final FollowupDisplayNameResolver displayNameResolver;
    private final ObjectMapper objectMapper;

    public PatientFollowupEntity get(Long id) {
        PatientFollowupEntity f = followupRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "随访档案不存在: " + id));
        // DEPT 范围详情入口（fail-closed）：患者任一就诊科室 == 用户科室才可见，否则同"未找到"
        deptScopeService.deptFilterName(com.maidc.common.core.enums.ErrorCode.PATIENT_NOT_FOUND)
                .filter(dept -> !encounterRepository.existsByPatientIdAndDepartmentAndIsDeletedFalse(f.getPatientId(), dept))
                .ifPresent(x -> { throw new BusinessException(404, "随访档案不存在: " + id); });
        displayNameResolver.fill(List.of(f));
        return f;
    }

    public Page<PatientFollowupEntity> list(Long cohortId, String status, int page, int size) {
        PageRequest pr = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        // DEPT 范围（doctor/nurse）只可见本科室患者的档案；SELF/ALL 不加过滤
        var dept = deptScopeService.deptFilterName(com.maidc.common.core.enums.ErrorCode.PATIENT_NOT_FOUND);
        Page<PatientFollowupEntity> result = dept.isPresent()
                ? (status != null && !status.isBlank()
                        ? followupRepository.findByCohortIdAndStatusAndDept(cohortId, status, dept.get(), pr)
                        : followupRepository.findByCohortIdAndDept(cohortId, dept.get(), pr))
                : (status != null && !status.isBlank()
                        ? followupRepository.findByCohortIdAndStatusAndIsDeletedFalse(cohortId, status, pr)
                        : followupRepository.findByCohortIdAndIsDeletedFalse(cohortId, pr));
        displayNameResolver.fill(result.getContent());
        return result;
    }

    /** 建档：校验在队列 → 快照 PUBLISHED 方案 → 按 stages 全量生成任务 */
    @Transactional
    public Map<String, Object> enroll(Long cohortId, Map<String, Object> req) {
        Long patientId = toLong(req.get("patientId"), "patientId");
        Long doctorId = toLong(req.get("doctorId"), "doctorId");
        Long nurseId = req.get("nurseId") == null ? null : toLong(req.get("nurseId"), "nurseId");
        LocalDate enrollDate = req.get("enrollDate") == null ? LocalDate.now()
                : LocalDate.parse(String.valueOf(req.get("enrollDate")));

        if (!cohortPatientRepository.existsByCohortIdAndPatientId(cohortId, patientId)) {
            // 不暴露存在性：统一 404
            throw new BusinessException(404, "患者不在该专病队列中，无法建档");
        }
        if (followupRepository.findByCohortIdAndPatientIdAndStatusAndIsDeletedFalse(cohortId, patientId, "ACTIVE").isPresent()) {
            throw new BusinessException(409, "该患者已有进行中的随访档案");
        }

        FollowupProtocolEntity protocol = protocolService.latestPublished(cohortId);
        validateProtocolScalesActive(protocol);

        PatientFollowupEntity followup = new PatientFollowupEntity();
        followup.setCohortId(cohortId);
        followup.setPatientId(patientId);
        followup.setProtocolId(protocol.getId());
        followup.setProtocolVersion(protocol.getVersion());
        followup.setDoctorId(doctorId);
        followup.setNurseId(nurseId);
        followup.setStatus("ACTIVE");
        followup.setEnrollDate(enrollDate);
        if (followup.getOrgId() == null) followup.setOrgId(0L);
        PatientFollowupEntity saved = followupRepository.save(followup);

        List<FollowupTaskEntity> tasks = generateTasks(saved, protocol);
        log.info("建档成功: followup={} patient={} 生成 {} 个阶段任务", saved.getId(), patientId, tasks.size());
        return Map.of("followupId", saved.getId(), "taskCount", tasks.size(),
                "protocolVersion", protocol.getVersion());
    }

    /** 任务生成：due = enroll_date + offsetDays；快照量表码 */
    private List<FollowupTaskEntity> generateTasks(PatientFollowupEntity followup, FollowupProtocolEntity protocol) {
        JsonNode stages = parse(protocol.getStages());
        List<FollowupTaskEntity> tasks = new ArrayList<>();
        for (JsonNode stage : stages) {
            tasks.add(buildTask(followup, stage));
        }
        return taskRepository.saveAll(tasks);
    }

    private FollowupTaskEntity buildTask(PatientFollowupEntity followup, JsonNode stage) {
        FollowupTaskEntity task = new FollowupTaskEntity();
        task.setFollowupId(followup.getId());
        task.setStageCode(stage.path("stageCode").asText());
        task.setStageName(stage.path("name").asText(""));
        task.setDueDate(followup.getEnrollDate().plusDays(stage.path("offsetDays").asLong(0)));
        task.setStatus("PENDING");
        task.setRequiredScales(stage.path("requiredScales").toString());
        task.setOptionalScales(stage.path("optionalScales").toString());
        if (task.getOrgId() == null) task.setOrgId(0L);
        return task;
    }

    /** 建档时方案含 DISABLED 量表 → 400 阻止 */
    private void validateProtocolScalesActive(FollowupProtocolEntity protocol) {
        for (JsonNode stage : parse(protocol.getStages())) {
            for (JsonNode code : stage.path("requiredScales")) {
                if (scaleRepository.findFirstByScaleCodeAndStatusAndIsDeletedFalseOrderByVersionDesc(code.asText(), "ACTIVE").isEmpty()) {
                    throw new BusinessException(400, "方案引用了已停用量表: " + code.asText() + "，无法建档");
                }
            }
        }
    }

    // ==================== 状态机 ====================

    @Transactional
    public PatientFollowupEntity suspend(Long id) {
        PatientFollowupEntity f = get(id);
        requireStatus(f, "ACTIVE", "仅进行中的档案可暂停");
        f.setStatus("SUSPENDED");
        return followupRepository.save(f);
    }

    @Transactional
    public PatientFollowupEntity resume(Long id) {
        PatientFollowupEntity f = get(id);
        requireStatus(f, "SUSPENDED", "仅已暂停的档案可恢复");
        f.setStatus("ACTIVE");
        return followupRepository.save(f);
    }

    @Transactional
    public PatientFollowupEntity close(Long id, String reason) {
        if (reason == null || reason.isBlank()) throw new BusinessException(400, "结案原因必填");
        PatientFollowupEntity f = get(id);
        if ("CLOSED".equals(f.getStatus()) || "OUT_OF_COHORT".equals(f.getStatus())) {
            throw new BusinessException(409, "档案已终结，不可重复操作");
        }
        f.setStatus("CLOSED");
        f.setCloseReason(reason);
        return followupRepository.save(f);
    }

    /** 队列重同步后患者脱组：数据全保留，任务冻结只读 */
    @Transactional
    public PatientFollowupEntity markOutOfCohort(Long id) {
        PatientFollowupEntity f = get(id);
        if ("ACTIVE".equals(f.getStatus()) || "SUSPENDED".equals(f.getStatus())) {
            f.setStatus("OUT_OF_COHORT");
            f.setOutOfCohortAt(java.time.LocalDateTime.now());
            return followupRepository.save(f);
        }
        return f;
    }

    /**
     * 升级方案：切换到最新 PUBLISHED 版本，只重建未来未完成任务
     * （PENDING 且 due_date ≥ 今天删除重建；DONE/SKIPPED 与已过期任务不动）。
     */
    @Transactional
    public Map<String, Object> upgradeProtocol(Long id) {
        PatientFollowupEntity f = get(id);
        if (!"ACTIVE".equals(f.getStatus())) throw new BusinessException(409, "仅进行中的档案可升级方案");

        FollowupProtocolEntity latest = protocolService.latestPublished(f.getCohortId());
        if (latest.getVersion().equals(f.getProtocolVersion())) {
            throw new BusinessException(400, "当前已是最新方案版本 v" + f.getProtocolVersion());
        }
        validateProtocolScalesActive(latest);

        LocalDate today = LocalDate.now();
        List<FollowupTaskEntity> futureTasks = taskRepository
                .findByFollowupIdAndDueDateGreaterThanEqualAndIsDeletedFalse(id, today).stream()
                .filter(t -> "PENDING".equals(t.getStatus()))
                .toList();
        // 只重建被删除任务对应的阶段（SKIPPED/已完成阶段不复活）
        Set<String> rebuildStageCodes = futureTasks.stream()
                .map(FollowupTaskEntity::getStageCode)
                .collect(Collectors.toSet());
        futureTasks.forEach(taskRepository::delete);

        f.setProtocolId(latest.getId());
        f.setProtocolVersion(latest.getVersion());
        followupRepository.save(f);

        List<FollowupTaskEntity> rebuilt = new ArrayList<>();
        for (JsonNode stage : parse(latest.getStages())) {
            if (!rebuildStageCodes.contains(stage.path("stageCode").asText())) continue;
            rebuilt.add(buildTask(f, stage));
        }
        taskRepository.saveAll(rebuilt);
        return Map.of("protocolVersion", latest.getVersion(),
                "removedTasks", futureTasks.size(), "rebuiltTasks", rebuilt.size());
    }

    private void requireStatus(PatientFollowupEntity f, String expected, String msg) {
        if (!expected.equals(f.getStatus())) throw new BusinessException(409, msg + "（当前状态 " + f.getStatus() + "）");
    }

    private JsonNode parse(String json) {
        try { return objectMapper.readTree(json); }
        catch (Exception e) { throw new BusinessException(500, "方案 stages 解析失败"); }
    }

    private Long toLong(Object o, String field) {
        if (o == null) throw new BusinessException(400, field + " 必填");
        try { return Long.parseLong(String.valueOf(o)); }
        catch (NumberFormatException e) { throw new BusinessException(400, field + " 非法: " + o); }
    }
}
