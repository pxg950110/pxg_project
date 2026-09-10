package com.maidc.data.service.followup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.FollowupTaskEntity;
import com.maidc.data.entity.PatientEntity;
import com.maidc.data.entity.PatientFollowupEntity;
import com.maidc.data.entity.ScaleAssessmentEntity;
import com.maidc.data.entity.ScaleDefinitionEntity;
import com.maidc.data.entity.TreatmentRecordEntity;
import com.maidc.data.repository.FollowupTaskRepository;
import com.maidc.data.repository.PatientFollowupRepository;
import com.maidc.data.repository.PatientRepository;
import com.maidc.data.repository.ScaleAssessmentRepository;
import com.maidc.data.repository.TreatmentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务执行：工作台视图 + complete（assessments 批量 + task DONE 同一事务）+ skip。
 * OVERDUE 为派生态（due_date < today && PENDING），不落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowupTaskService {

    private final FollowupTaskRepository taskRepository;
    private final PatientFollowupRepository followupRepository;
    private final ScaleAssessmentRepository assessmentRepository;
    private final TreatmentRecordRepository treatmentRepository;
    private final ScaleDefinitionService scaleService;
    private final ScaleEngine scaleEngine;
    private final ObjectMapper objectMapper;
    private final DeptScopeService deptScopeService;
    private final FollowupDisplayNameResolver displayNameResolver;

    /** 工作台：我负责的（nurse 优先 / doctor 兜底）或全量（DEPT 范围限本科室患者），按 今日/超期/未来7天 分组 */
    public Map<String, Object> myWorkbench(Long userId, boolean all, LocalDate today) {
        List<Long> followupIds = new ArrayList<>();
        var dept = deptScopeService.deptFilterName(com.maidc.common.core.enums.ErrorCode.PATIENT_NOT_FOUND);
        if (!all) {
            List<PatientFollowupEntity> mine = followupRepository.findByNurseIdAndStatusAndIsDeletedFalse(userId, "ACTIVE");
            if (mine.isEmpty()) {
                mine = followupRepository.findByDoctorIdAndStatusAndIsDeletedFalse(userId, "ACTIVE");
            }
            // DEPT 范围下再按本科室过滤（∃ 语义）
            if (dept.isPresent()) {
                java.util.Set<Long> deptVisible = followupRepository
                        .findActiveWithPendingDueAndDept(today, dept.get()).stream()
                        .map(PatientFollowupEntity::getId).collect(java.util.stream.Collectors.toSet());
                mine = mine.stream().filter(f -> deptVisible.contains(f.getId())).toList();
            }
            mine.forEach(f -> followupIds.add(f.getId()));
            if (followupIds.isEmpty()) return emptyWorkbench();
        }

        LocalDate horizon = today.plusDays(7);
        List<FollowupTaskEntity> pending = all
                ? taskRepository.findByFollowupIdInAndStatusAndIsDeletedFalse(
                        dept.isPresent()
                                ? followupRepository.findActiveWithPendingDueAndDept(today, dept.get()).stream()
                                        .map(PatientFollowupEntity::getId).toList()
                                : allActiveFollowupIds(),
                        "PENDING")
                : taskRepository.findByFollowupIdInAndStatusAndIsDeletedFalse(followupIds, "PENDING");

        List<Long> fids = pending.stream().map(FollowupTaskEntity::getFollowupId).distinct().toList();
        Map<Long, Map<String, Object>> patientInfo = buildPatientInfo(fids);

        Map<String, Object> result = new HashMap<>();
        result.put("today", pending.stream().filter(t -> t.getDueDate().isEqual(today)).map(t -> toView(t, patientInfo)).toList());
        result.put("overdue", pending.stream().filter(t -> t.getDueDate().isBefore(today)).map(t -> toView(t, patientInfo)).toList());
        result.put("upcoming", pending.stream().filter(t -> t.getDueDate().isAfter(today) && !t.getDueDate().isAfter(horizon)).map(t -> toView(t, patientInfo)).toList());
        result.put("stats", Map.of(
                "todayCount", pending.stream().filter(t -> t.getDueDate().isEqual(today)).count(),
                "overdueCount", pending.stream().filter(t -> t.getDueDate().isBefore(today)).count(),
                "upcomingCount", pending.stream().filter(t -> t.getDueDate().isAfter(today) && !t.getDueDate().isAfter(horizon)).count()));
        return result;
    }

    /** 首页工作台聚合摘要：我负责的随访统计（今日/超期/本周完成/在管患者）+ 任务行（含患者姓名富化） */
    public Map<String, Object> workspaceDigest(Long userId, LocalDate today) {
        List<PatientFollowupEntity> mine = followupRepository.findByNurseIdAndStatusAndIsDeletedFalse(userId, "ACTIVE");
        if (mine.isEmpty()) {
            mine = followupRepository.findByDoctorIdAndStatusAndIsDeletedFalse(userId, "ACTIVE");
        }
        Map<String, Object> digest = new HashMap<>();
        digest.put("todayCount", 0L);
        digest.put("overdueCount", 0L);
        digest.put("weekDoneCount", 0L);
        digest.put("activePatients", 0L);
        digest.put("tasks", List.of());
        if (mine.isEmpty()) {
            return digest;
        }

        List<Long> followupIds = mine.stream().map(PatientFollowupEntity::getId).toList();
        List<FollowupTaskEntity> pending = taskRepository.findByFollowupIdInAndStatusAndIsDeletedFalse(followupIds, "PENDING");

        Map<Long, Long> followupToPatient = mine.stream()
                .collect(Collectors.toMap(PatientFollowupEntity::getId, PatientFollowupEntity::getPatientId));
        Map<Long, String> patientNames = displayNameResolver
                .loadPatients(new java.util.ArrayList<>(new java.util.HashSet<>(followupToPatient.values())))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue().get("name"))));

        LocalDate horizon = today.plusDays(7);
        List<Map<String, Object>> tasks = pending.stream()
                .filter(t -> !t.getDueDate().isAfter(horizon))
                .map(t -> {
                    Map<String, Object> row = new HashMap<>(toView(t));
                    Long patientId = followupToPatient.get(t.getFollowupId());
                    row.put("patientId", patientId);
                    row.put("patientName", patientNames.getOrDefault(patientId, "患者#" + patientId));
                    return row;
                })
                .sorted(Comparator.comparing(row -> LocalDate.parse((String) row.get("dueDate"))))
                .toList();

        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        digest.put("todayCount", pending.stream().filter(t -> t.getDueDate().isEqual(today)).count());
        digest.put("overdueCount", pending.stream().filter(t -> t.getDueDate().isBefore(today)).count());
        digest.put("weekDoneCount", (long) taskRepository
                .findByStatusAndCompletedByAndCompletedAtGreaterThanEqualAndIsDeletedFalse("DONE", userId, weekStart).size());
        digest.put("activePatients", (long) mine.size());
        digest.put("tasks", tasks);
        return digest;
    }

    private List<Long> allActiveFollowupIds() {
        return followupRepository.findAll().stream()
                .filter(f -> Boolean.FALSE.equals(f.getIsDeleted()) && "ACTIVE".equals(f.getStatus()))
                .map(PatientFollowupEntity::getId).toList();
    }

    /** followupId -> {patientId,patientName,gender,age}（批量查患者表） */
    private Map<Long, Map<String, Object>> buildPatientInfo(List<Long> followupIds) {
        if (followupIds.isEmpty()) return Map.of();
        List<PatientFollowupEntity> entities = followupRepository.findAllById(followupIds).stream()
                .filter(f -> Boolean.FALSE.equals(f.getIsDeleted())).toList();
        if (entities.isEmpty()) return Map.of();
        Map<Long, Map<String, Object>> patients = displayNameResolver.loadPatients(
                entities.stream().map(PatientFollowupEntity::getPatientId).distinct().toList());
        Map<Long, Map<String, Object>> info = new HashMap<>();
        LocalDate today = LocalDate.now();
        for (PatientFollowupEntity f : entities) {
            Map<String, Object> p = patients.get(f.getPatientId());
            if (p == null) continue;
            Map<String, Object> pi = new HashMap<>();
            pi.put("patientId", f.getPatientId());
            pi.put("patientName", p.get("name"));
            pi.put("gender", p.get("gender"));
            Object birth = p.get("birth_date");
            pi.put("age", birth instanceof java.sql.Date d ? java.time.Period.between(d.toLocalDate(), today).getYears() : null);
            info.put(f.getId(), pi);
        }
        return info;
    }

    private Map<String, Object> emptyWorkbench() {
        return Map.of("today", List.of(), "overdue", List.of(), "upcoming", List.of(),
                "stats", Map.of("todayCount", 0, "overdueCount", 0, "upcomingCount", 0));
    }

    /** 档案任务时间轴（OVERDUE 派生） */
    public List<Map<String, Object>> timeline(Long followupId) {
        return taskRepository.findByFollowupIdAndIsDeletedFalseOrderByDueDateAsc(followupId).stream()
                .map(this::toView).toList();
    }

    /** 档案下 PENDING 任务实体（提醒扫描用） */
    public List<FollowupTaskEntity> pendingTasks(Long followupId) {
        return taskRepository.findByFollowupIdInAndStatusAndIsDeletedFalse(List.of(followupId), "PENDING");
    }

    /**
     * 完成任务：assessments 批量 + 可选 treatments + task DONE 同一事务。
     * 边界：任务非 PENDING → 409；档案冻结 → 409；量表/答案非法 → 400；重复量表 → 409。
     */
    @Transactional
    public Map<String, Object> complete(Long taskId, Map<String, Object> req, Long userId) {
        FollowupTaskEntity task = taskRepository.findByIdAndIsDeletedFalse(taskId)
                .orElseThrow(() -> new BusinessException(404, "任务不存在: " + taskId));
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException(409, "任务已完成或已跳过，不可重复提交");
        }
        PatientFollowupEntity followup = followupRepository.findByIdAndIsDeletedFalse(task.getFollowupId())
                .orElseThrow(() -> new BusinessException(404, "随访档案不存在"));
        if (!"ACTIVE".equals(followup.getStatus())) {
            throw new BusinessException(409, "档案已冻结（" + followup.getStatus() + "），任务不可执行");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> assessments = (List<Map<String, Object>>) req.getOrDefault("assessments", List.of());
        // 必评量表全覆盖校验
        List<String> required = parseStringArray(task.getRequiredScales());
        List<String> answered = assessments.stream().map(a -> String.valueOf(a.get("scaleCode"))).toList();
        for (String code : required) {
            if (!answered.contains(code)) throw new BusinessException(400, "必评量表未提交: " + code);
        }

        LocalDateTime now = LocalDateTime.now();
        List<ScaleAssessmentEntity> savedAssessments = new ArrayList<>();
        for (Map<String, Object> a : assessments) {
            savedAssessments.add(buildAssessment(followup, task, a, userId, now));
        }
        assessmentRepository.saveAll(savedAssessments);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> treatments = (List<Map<String, Object>>) req.getOrDefault("treatments", List.of());
        for (Map<String, Object> t : treatments) {
            treatmentRepository.save(buildTreatment(followup, task, t, "MANUAL"));
        }

        task.setStatus("DONE");
        task.setCompletedAt(now);
        task.setCompletedBy(userId);
        taskRepository.save(task);
        log.info("任务完成: task={} followup={} 量表 {} 条 治疗 {} 条", taskId, followup.getId(), savedAssessments.size(), treatments.size());
        return Map.of("taskId", taskId, "assessments", savedAssessments.size(), "treatments", treatments.size());
    }

    /** 跳过（医生，原因必填） */
    @Transactional
    public FollowupTaskEntity skip(Long taskId, String reason, Long userId) {
        if (reason == null || reason.isBlank()) throw new BusinessException(400, "跳过原因必填");
        FollowupTaskEntity task = taskRepository.findByIdAndIsDeletedFalse(taskId)
                .orElseThrow(() -> new BusinessException(404, "任务不存在: " + taskId));
        if (!"PENDING".equals(task.getStatus())) throw new BusinessException(409, "任务已终结，不可跳过");
        task.setStatus("SKIPPED");
        task.setSkipReason(reason);
        task.setCompletedAt(LocalDateTime.now());
        task.setCompletedBy(userId);
        return taskRepository.save(task);
    }

    /** 计划外评估（task_id=NULL，不受 UNIQUE 约束） */
    @Transactional
    public ScaleAssessmentEntity planFreeAssessment(Long followupId, Map<String, Object> req, Long userId) {
        PatientFollowupEntity followup = followupRepository.findByIdAndIsDeletedFalse(followupId)
                .orElseThrow(() -> new BusinessException(404, "随访档案不存在"));
        if ("OUT_OF_COHORT".equals(followup.getStatus()) || "CLOSED".equals(followup.getStatus())) {
            throw new BusinessException(409, "档案已终结，不可发起评估");
        }
        return assessmentRepository.save(buildAssessment(followup, null, req, userId, LocalDateTime.now()));
    }

    public List<ScaleAssessmentEntity> assessments(Long followupId) {
        return assessmentRepository.findByFollowupIdAndIsDeletedFalseOrderByAssessedAtAsc(followupId);
    }

    // ==================== 组装 ====================

    private ScaleAssessmentEntity buildAssessment(PatientFollowupEntity followup, FollowupTaskEntity task,
                                                  Map<String, Object> a, Long userId, LocalDateTime now) {
        String scaleCode = String.valueOf(a.get("scaleCode"));
        if (task != null && assessmentRepository.existsByTaskIdAndScaleCodeAndIsDeletedFalse(task.getId(), scaleCode)) {
            throw new BusinessException(409, "该任务下量表已评估: " + scaleCode);
        }
        ScaleDefinitionEntity scale = scaleService.latestActive(scaleCode);
        JsonNode def = scaleEngine.parse(scale.getDefinition());

        @SuppressWarnings("unchecked")
        Map<String, Object> answers = (Map<String, Object>) a.get("answers");
        if (answers == null) throw new BusinessException(400, "answers 不能为空: " + scaleCode);
        scaleEngine.validateAnswers(def, answers);
        int total = scaleEngine.score(def, answers);

        ScaleAssessmentEntity entity = new ScaleAssessmentEntity();
        entity.setFollowupId(followup.getId());
        entity.setTaskId(task == null ? null : task.getId());
        entity.setScaleCode(scaleCode);
        entity.setScaleVersion(scale.getVersion());
        entity.setAnswers(scaleEngine.normalizeAnswers(def, answers));
        if (a.get("bindingSources") != null) entity.setBindingSources(toJson(a.get("bindingSources")));
        entity.setTotalScore(total);
        entity.setAssessedAt(now);
        entity.setAssessedBy(userId);
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        return entity;
    }

    private TreatmentRecordEntity buildTreatment(PatientFollowupEntity followup, FollowupTaskEntity task,
                                                 Map<String, Object> t, String source) {
        TreatmentRecordEntity entity = new TreatmentRecordEntity();
        entity.setFollowupId(followup.getId());
        entity.setTaskId(task == null ? null : task.getId());
        entity.setCategory(String.valueOf(t.getOrDefault("category", "OTHER")));
        entity.setName(String.valueOf(t.get("name")));
        if (entity.getName().isBlank()) throw new BusinessException(400, "治疗名称必填");
        entity.setOccurredDate(t.get("occurredDate") == null ? LocalDate.now()
                : LocalDate.parse(String.valueOf(t.get("occurredDate"))));
        entity.setSource(source);
        if (t.get("detail") != null) entity.setDetail(toJson(t.get("detail")));
        if (t.get("sourceRef") != null) entity.setSourceRef(toJson(t.get("sourceRef")));
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        return entity;
    }

    private Map<String, Object> toView(FollowupTaskEntity t) {
        return toView(t, null);
    }

    /** view 附带患者信息（patientInfo: followupId -> {patientId,patientName,gender,age}） */
    private Map<String, Object> toView(FollowupTaskEntity t, Map<Long, Map<String, Object>> patientInfo) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", t.getId());
        view.put("followupId", t.getFollowupId());
        Map<String, Object> pi = patientInfo == null ? null : patientInfo.get(t.getFollowupId());
        if (pi != null) {
            view.put("patientId", pi.get("patientId"));
            view.put("patientName", pi.get("patientName"));
            view.put("gender", pi.get("gender"));
            view.put("age", pi.get("age"));
        }
        view.put("stageCode", t.getStageCode());
        view.put("stageName", t.getStageName());
        view.put("dueDate", t.getDueDate().toString());
        view.put("status", "PENDING".equals(t.getStatus()) && t.getDueDate().isBefore(LocalDate.now())
                ? "OVERDUE" : t.getStatus());
        view.put("requiredScales", parseStringArray(t.getRequiredScales()));
        view.put("optionalScales", parseStringArray(t.getOptionalScales()));
        view.put("overdueDays", "PENDING".equals(t.getStatus()) && t.getDueDate().isBefore(LocalDate.now())
                ? LocalDate.now().toEpochDay() - t.getDueDate().toEpochDay() : 0);
        return view;
    }

    private List<String> parseStringArray(String json) {
        try {
            JsonNode node = objectMapper.readTree(json == null ? "[]" : json);
            List<String> list = new ArrayList<>();
            node.forEach(n -> list.add(n.asText()));
            return list;
        } catch (Exception e) { return List.of(); }
    }

    private String toJson(Object o) {
        try { return o instanceof String s ? s : objectMapper.writeValueAsString(o); }
        catch (Exception e) { throw new BusinessException(400, "JSON 序列化失败"); }
    }
}
