package com.maidc.data.service.followup;

import com.maidc.data.entity.FollowupTaskEntity;
import com.maidc.data.entity.PatientFollowupEntity;
import com.maidc.data.entity.ScaleAssessmentEntity;
import com.maidc.data.entity.TreatmentRecordEntity;
import com.maidc.data.repository.FollowupTaskRepository;
import com.maidc.data.repository.PatientFollowupRepository;
import com.maidc.data.repository.ScaleAssessmentRepository;
import com.maidc.data.repository.TreatmentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 结局看板聚合：无独立统计表，全部从 assessment/treatment/task 实时聚合。
 * 指标：依从率 / SNOT-22 改善率（MCID 9）/ 手术率 / 再手术 / 量表趋势 / 药物分布。
 */
@Service
@RequiredArgsConstructor
public class OutcomeStatsService {

    private static final String SNOT22 = "SNOT22";
    private static final int MCID = 9;

    private final PatientFollowupRepository followupRepository;
    private final FollowupTaskRepository taskRepository;
    private final ScaleAssessmentRepository assessmentRepository;
    private final TreatmentRecordRepository treatmentRepository;

    public Map<String, Object> stats(Long cohortId) {
        List<PatientFollowupEntity> followups = followupRepository.findAll().stream()
                .filter(f -> Boolean.FALSE.equals(f.getIsDeleted()) && Objects.equals(f.getCohortId(), cohortId))
                .toList();
        List<Long> ids = followups.stream().map(PatientFollowupEntity::getId).toList();
        if (ids.isEmpty()) return emptyStats();

        List<FollowupTaskEntity> tasks = ids.stream()
                .flatMap(fid -> taskRepository.findByFollowupIdAndIsDeletedFalseOrderByDueDateAsc(fid).stream())
                .toList();
        List<ScaleAssessmentEntity> assessments = ids.stream()
                .flatMap(fid -> assessmentRepository.findByFollowupIdAndIsDeletedFalseOrderByAssessedAtAsc(fid).stream())
                .toList();
        List<TreatmentRecordEntity> treatments = ids.stream()
                .flatMap(fid -> treatmentRepository.findByFollowupIdAndIsDeletedFalseOrderByOccurredDateDesc(fid).stream())
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalFollowups", followups.size());
        result.put("complianceRate", complianceRate(tasks));
        result.put("improvementRate", improvementRate(assessments, tasks));
        result.put("surgeryRate", surgeryRate(followups.size(), treatments));
        result.put("reoperationCount", reoperationCount(treatments));
        result.put("scaleTrends", scaleTrends(assessments, tasks));
        result.put("medicationDistribution", medicationDistribution(treatments));
        return result;
    }

    /** 应完成任务（due ≤ 统计日）中 DONE 占比 */
    private double complianceRate(List<FollowupTaskEntity> tasks) {
        LocalDate today = LocalDate.now();
        long due = tasks.stream().filter(t -> !t.getDueDate().isAfter(today)).count();
        if (due == 0) return 0;
        long done = tasks.stream().filter(t -> !t.getDueDate().isAfter(today) && "DONE".equals(t.getStatus())).count();
        return Math.round(done * 1000.0 / due) / 10.0;
    }

    /** 有基线且末次评估距基线 ≥ 3 个月的患者中，基线 − 末次 ≥ MCID 占比 */
    private double improvementRate(List<ScaleAssessmentEntity> assessments, List<FollowupTaskEntity> tasks) {
        Map<Long, List<ScaleAssessmentEntity>> byFollowup = assessments.stream()
                .filter(a -> SNOT22.equals(a.getScaleCode()))
                .collect(Collectors.groupingBy(ScaleAssessmentEntity::getFollowupId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, String> taskStage = tasks.stream()
                .filter(t -> t.getStageCode() != null)
                .collect(Collectors.toMap(FollowupTaskEntity::getId, FollowupTaskEntity::getStageCode, (a, b) -> a));

        long eligible = 0, improved = 0;
        for (List<ScaleAssessmentEntity> list : byFollowup.values()) {
            Optional<ScaleAssessmentEntity> baseline = list.stream()
                    .filter(a -> a.getTaskId() == null || "BASELINE".equals(taskStage.get(a.getTaskId())))
                    .findFirst();
            if (baseline.isEmpty()) continue;
            ScaleAssessmentEntity first = baseline.get();
            Optional<ScaleAssessmentEntity> last = list.stream()
                    .filter(a -> ChronoUnit.MONTHS.between(first.getAssessedAt(), a.getAssessedAt()) >= 3)
                    .reduce((a, b) -> b);
            if (last.isEmpty()) continue;
            eligible++;
            if (first.getTotalScore() - last.get().getTotalScore() >= MCID) improved++;
        }
        return eligible == 0 ? 0 : Math.round(improved * 1000.0 / eligible) / 10.0;
    }

    private double surgeryRate(int followupCount, List<TreatmentRecordEntity> treatments) {
        if (followupCount == 0) return 0;
        long operated = treatments.stream().filter(t -> "SURGERY".equals(t.getCategory()))
                .map(TreatmentRecordEntity::getFollowupId).distinct().count();
        return Math.round(operated * 1000.0 / followupCount) / 10.0;
    }

    private long reoperationCount(List<TreatmentRecordEntity> treatments) {
        Map<Long, Long> essByFollowup = treatments.stream()
                .filter(t -> "SURGERY".equals(t.getCategory()) && t.getName() != null && t.getName().toUpperCase().contains("ESS"))
                .collect(Collectors.groupingBy(TreatmentRecordEntity::getFollowupId, Collectors.counting()));
        return essByFollowup.values().stream().filter(c -> c >= 2).count();
    }

    /** 四量表按阶段（基线/3/6/12月）均分趋势 */
    private Map<String, Object> scaleTrends(List<ScaleAssessmentEntity> assessments, List<FollowupTaskEntity> tasks) {
        Map<Long, String> taskStage = tasks.stream()
                .filter(t -> t.getStageCode() != null)
                .collect(Collectors.toMap(FollowupTaskEntity::getId, FollowupTaskEntity::getStageCode, (a, b) -> a));
        Map<String, Map<String, List<Integer>>> byScaleStage = new LinkedHashMap<>();
        for (ScaleAssessmentEntity a : assessments) {
            String stage = a.getTaskId() == null ? null : taskStage.get(a.getTaskId());
            if (stage == null) stage = "BASELINE"; // 计划外/无任务关联按时间归类不到趋势
            byScaleStage.computeIfAbsent(a.getScaleCode(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(stage, k -> new ArrayList<>()).add(a.getTotalScore());
        }
        Map<String, Object> trends = new LinkedHashMap<>();
        for (String scale : List.of(SNOT22, "LUND_KENNEDY", "LUND_MACKAY", "OLFACTION")) {
            Map<String, List<Integer>> stages = byScaleStage.getOrDefault(scale, Map.of());
            List<Map<String, Object>> points = new ArrayList<>();
            for (String stage : List.of("BASELINE", "M3", "M6", "M12")) {
                List<Integer> scores = stages.getOrDefault(stage, List.of());
                if (!scores.isEmpty()) {
                    points.add(Map.of("stage", stage, "avg", scores.stream().mapToInt(Integer::intValue).average().orElse(0)));
                }
            }
            trends.put(scale, points);
        }
        return trends;
    }

    private List<Map<String, Object>> medicationDistribution(List<TreatmentRecordEntity> treatments) {
        Map<String, Long> counts = treatments.stream()
                .filter(t -> "MEDICATION".equals(t.getCategory()))
                .collect(Collectors.groupingBy(TreatmentRecordEntity::getName, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "count", e.getValue()))
                .collect(Collectors.toList());
    }

    private Map<String, Object> emptyStats() {
        return Map.of("totalFollowups", 0, "complianceRate", 0, "improvementRate", 0,
                "surgeryRate", 0, "reoperationCount", 0, "scaleTrends", Map.of(), "medicationDistribution", List.of());
    }
}
