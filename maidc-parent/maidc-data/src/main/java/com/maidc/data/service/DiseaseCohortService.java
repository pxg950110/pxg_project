package com.maidc.data.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.DiseaseCohortEntity;
import com.maidc.data.entity.DiseaseCohortPatientEntity;
import com.maidc.data.entity.PatientEntity;
import com.maidc.data.repository.DiseaseCohortPatientRepository;
import com.maidc.data.repository.DiseaseCohortRepository;
import com.maidc.data.repository.PatientRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiseaseCohortService {

    private final DiseaseCohortRepository cohortRepository;
    private final DiseaseCohortPatientRepository cohortPatientRepository;
    private final PatientRepository patientRepository;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    private static final Map<String, String> DOMAIN_TABLE_MAP = Map.of(
            "DIAGNOSIS", "cdr.c_diagnosis",
            "LAB", "cdr.c_lab_test",
            "MEDICATION", "cdr.c_medication",
            "IMAGING", "cdr.c_imaging_exam",
            "SURGERY", "cdr.c_operation",
            "PATHOLOGY", "cdr.c_pathology"
    );

    // field alias: template field name -> actual DB column name
    private static final Map<String, Map<String, String>> DOMAIN_FIELD_MAP = Map.of(
            "DIAGNOSIS", Map.of("diagnosis_code", "icd_code", "diagnosis_name", "icd_name"),
            "LAB", Map.of("test_code", "test_code", "test_name", "test_name"),
            "SURGERY", Map.of("operation_name", "operation_name", "operation_code", "operation_code"),
            "PATHOLOGY", Map.of("diagnosis_desc", "diagnosis_desc"),
            "IMAGING", Map.of("exam_type", "exam_type", "body_part", "body_part"),
            "MEDICATION", Map.of("med_name", "med_name", "med_code", "med_code")
    );

    // ==================== CRUD ====================

    public Page<DiseaseCohortEntity> listCohorts(String keyword, String status, int page, int size) {
        Specification<DiseaseCohortEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(root.get("name"), "%" + keyword + "%"));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return cohortRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public DiseaseCohortEntity getCohort(Long id) {
        return cohortRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "专病库不存在: " + id));
    }

    @Transactional
    public DiseaseCohortEntity createCohort(DiseaseCohortEntity entity) {
        entity.setPatientCount(0);
        entity.setStatus("ACTIVE");
        DiseaseCohortEntity saved = cohortRepository.save(entity);
        log.info("专病库创建成功: id={}, name={}", saved.getId(), saved.getName());
        // async match - run in background
        try {
            matchPatients(saved.getId());
        } catch (Exception e) {
            log.warn("首次匹配失败, 专病库id={}: {}", saved.getId(), e.getMessage());
        }
        return saved;
    }

    @Transactional
    public DiseaseCohortEntity updateCohort(Long id, DiseaseCohortEntity updates) {
        DiseaseCohortEntity entity = getCohort(id);
        if (updates.getName() != null) entity.setName(updates.getName());
        if (updates.getDescription() != null) entity.setDescription(updates.getDescription());
        if (updates.getInclusionRules() != null) {
            entity.setInclusionRules(updates.getInclusionRules());
            // rules changed, re-match
            try {
                matchPatients(id);
            } catch (Exception e) {
                log.warn("更新后匹配失败, 专病库id={}: {}", id, e.getMessage());
            }
        }
        if (updates.getStatus() != null) entity.setStatus(updates.getStatus());
        if (updates.getAutoSync() != null) entity.setAutoSync(updates.getAutoSync());
        return cohortRepository.save(entity);
    }

    @Transactional
    public void deleteCohort(Long id) {
        DiseaseCohortEntity entity = getCohort(id);
        // remove all patient associations
        cohortPatientRepository.findByCohortId(id)
                .forEach(cp -> cohortPatientRepository.delete(cp));
        cohortRepository.delete(entity);
        log.info("专病库已删除: id={}", id);
    }

    // ==================== 匹配引擎 ====================

    @Transactional
    public int matchPatients(Long cohortId) {
        DiseaseCohortEntity cohort = getCohort(cohortId);
        Set<Long> matchedPatientIds = executeMatching(cohort.getInclusionRules());

        // preserve MANUAL records
        List<DiseaseCohortPatientEntity> existing = cohortPatientRepository.findByCohortId(cohortId);
        Set<Long> manualPatientIds = existing.stream()
                .filter(cp -> "MANUAL".equals(cp.getMatchSource()))
                .map(DiseaseCohortPatientEntity::getPatientId)
                .collect(Collectors.toSet());

        // remove old AUTO records
        existing.stream()
                .filter(cp -> "AUTO".equals(cp.getMatchSource()))
                .forEach(cohortPatientRepository::delete);

        // insert new AUTO records
        Set<Long> allPatientIds = new HashSet<>(matchedPatientIds);
        allPatientIds.addAll(manualPatientIds);
        LocalDateTime now = LocalDateTime.now();
        for (Long patientId : matchedPatientIds) {
            if (!manualPatientIds.contains(patientId)) {
                DiseaseCohortPatientEntity cp = new DiseaseCohortPatientEntity();
                cp.setCohortId(cohortId);
                cp.setPatientId(patientId);
                cp.setMatchSource("AUTO");
                cp.setMatchedAt(now);
                cohortPatientRepository.save(cp);
            }
        }

        // update count
        cohort.setPatientCount(allPatientIds.size());
        cohort.setLastSyncAt(now);
        cohortRepository.save(cohort);

        log.info("匹配完成: cohortId={}, auto={}, manual={}, total={}",
                cohortId, matchedPatientIds.size(), manualPatientIds.size(), allPatientIds.size());
        return allPatientIds.size();
    }

    public int matchPreview(Long cohortId) {
        DiseaseCohortEntity cohort = getCohort(cohortId);
        return executeMatching(cohort.getInclusionRules()).size();
    }

    private Set<Long> executeMatching(String inclusionRulesJson) {
        try {
            JsonNode root = objectMapper.readTree(inclusionRulesJson);
            String groupLogic = root.path("groupLogic").asText("AND");
            JsonNode groups = root.path("groups");

            if (groups.isEmpty()) return Set.of();

            List<Set<Long>> groupResults = new ArrayList<>();

            try (Connection conn = dataSource.getConnection()) {
                for (JsonNode group : groups) {
                    Set<Long> patientIds = executeGroup(conn, group);
                    groupResults.add(patientIds);
                }
            }

            if (groupResults.isEmpty()) return Set.of();

            Set<Long> result = new HashSet<>(groupResults.get(0));
            for (int i = 1; i < groupResults.size(); i++) {
                if ("OR".equalsIgnoreCase(groupLogic)) {
                    result.addAll(groupResults.get(i));
                } else {
                    result.retainAll(groupResults.get(i));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("匹配引擎执行失败", e);
            return Set.of();
        }
    }

    private Set<Long> executeGroup(Connection conn, JsonNode group) throws Exception {
        String domain = group.path("domain").asText();
        String logic = group.path("logic").asText("OR");
        JsonNode conditions = group.path("conditions");

        String table = DOMAIN_TABLE_MAP.get(domain);
        if (table == null || conditions.isEmpty()) return Set.of();

        Map<String, String> fieldMap = DOMAIN_FIELD_MAP.getOrDefault(domain, Map.of());

        StringBuilder sql = new StringBuilder("SELECT DISTINCT patient_id FROM ");
        sql.append(table).append(" WHERE is_deleted = false AND (");

        List<Object> params = new ArrayList<>();
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) sql.append(" ").append(logic).append(" ");
            JsonNode cond = conditions.get(i);
            String field = cond.path("field").asText();
            String op = cond.path("operator").asText();
            String dbField = fieldMap.getOrDefault(field, field);

            switch (op.toUpperCase()) {
                case "LIKE" -> {
                    sql.append(dbField).append(" LIKE ?");
                    params.add(cond.path("value").asText());
                }
                case "IN" -> {
                    JsonNode values = cond.path("value");
                    if (values.isArray()) {
                        List<String> vals = new ArrayList<>();
                        for (JsonNode v : values) vals.add(v.asText());
                        String placeholders = vals.stream().map(v -> "?").collect(Collectors.joining(","));
                        sql.append(dbField).append(" IN (").append(placeholders).append(")");
                        params.addAll(vals);
                    }
                }
                case "CONTAINS" -> {
                    sql.append(dbField).append(" LIKE ?");
                    params.add("%" + cond.path("value").asText() + "%");
                }
                case "=" -> {
                    sql.append(dbField).append(" = ?");
                    params.add(cond.path("value").asText());
                }
                default -> sql.append("1=1");
            }
        }
        sql.append(")");

        Set<Long> patientIds = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, (String) params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    patientIds.add(rs.getLong("patient_id"));
                }
            }
        }
        return patientIds;
    }

    // ==================== 患者管理 ====================

    public Map<String, Object> getPatients(Long cohortId, int page, int size) {
        List<DiseaseCohortPatientEntity> all = cohortPatientRepository.findByCohortId(cohortId);
        int total = all.size();
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<DiseaseCohortPatientEntity> pageContent = all.subList(fromIndex, toIndex);

        Set<Long> patientIds = pageContent.stream()
                .map(DiseaseCohortPatientEntity::getPatientId)
                .collect(Collectors.toSet());
        Map<Long, PatientEntity> patientMap = patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(PatientEntity::getId, p -> p));

        List<Map<String, Object>> items = pageContent.stream().map(cp -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", cp.getId());
            item.put("patientId", cp.getPatientId());
            item.put("matchSource", cp.getMatchSource());
            item.put("matchedAt", cp.getMatchedAt());
            PatientEntity patient = patientMap.get(cp.getPatientId());
            if (patient != null) {
                item.put("patientName", patient.getName());
                item.put("gender", patient.getGender());
                if (patient.getBirthDate() != null) {
                    item.put("age", Period.between(patient.getBirthDate(), java.time.LocalDate.now()).getYears());
                }
            }
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", size);
        return result;
    }

    @Transactional
    public void addPatient(Long cohortId, Long patientId) {
        if (cohortPatientRepository.existsByCohortIdAndPatientId(cohortId, patientId)) {
            throw new BusinessException(400, "患者已在专病库中");
        }
        DiseaseCohortPatientEntity cp = new DiseaseCohortPatientEntity();
        cp.setCohortId(cohortId);
        cp.setPatientId(patientId);
        cp.setMatchSource("MANUAL");
        cp.setMatchedAt(LocalDateTime.now());
        cohortPatientRepository.save(cp);

        // update count
        DiseaseCohortEntity cohort = getCohort(cohortId);
        cohort.setPatientCount((int) cohortPatientRepository.countByCohortId(cohortId));
        cohortRepository.save(cohort);
    }

    @Transactional
    public void removePatient(Long cohortId, Long patientId) {
        DiseaseCohortPatientEntity cp = cohortPatientRepository.findByCohortId(cohortId).stream()
                .filter(e -> e.getPatientId().equals(patientId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(404, "关联记录不存在"));

        if ("AUTO".equals(cp.getMatchSource())) {
            throw new BusinessException(400, "自动匹配的患者不能手动移除，请先同步更新规则");
        }
        cohortPatientRepository.delete(cp);

        DiseaseCohortEntity cohort = getCohort(cohortId);
        cohort.setPatientCount((int) cohortPatientRepository.countByCohortId(cohortId));
        cohortRepository.save(cohort);
    }

    // ==================== 统计 ====================

    public Map<String, Object> getStatistics(Long cohortId) {
        List<DiseaseCohortPatientEntity> cps = cohortPatientRepository.findByCohortId(cohortId);
        Set<Long> patientIds = cps.stream().map(DiseaseCohortPatientEntity::getPatientId).collect(Collectors.toSet());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalPatients", patientIds.size());

        if (patientIds.isEmpty()) {
            stats.put("maleRatio", 0.0);
            stats.put("avgAge", 0);
            stats.put("recentCount", 0);
            return stats;
        }

        List<PatientEntity> patients = patientRepository.findAllById(patientIds);
        long maleCount = patients.stream().filter(p -> "M".equals(p.getGender())).count();
        stats.put("maleRatio", patients.isEmpty() ? 0.0 : Math.round(maleCount * 10000.0 / patients.size()) / 100.0);

        double avgAge = patients.stream()
                .filter(p -> p.getBirthDate() != null)
                .mapToInt(p -> Period.between(p.getBirthDate(), java.time.LocalDate.now()).getYears())
                .average().orElse(0);
        stats.put("avgAge", Math.round(avgAge * 10.0) / 10.0);

        // recent 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentCount = cps.stream()
                .filter(cp -> cp.getMatchedAt() != null && cp.getMatchedAt().isAfter(thirtyDaysAgo))
                .count();
        stats.put("recentCount", recentCount);

        return stats;
    }

    // ==================== 导出 ====================

    public void exportCsv(Long cohortId, HttpServletResponse response) throws IOException {
        DiseaseCohortEntity cohort = getCohort(cohortId);
        List<DiseaseCohortPatientEntity> cps = cohortPatientRepository.findByCohortId(cohortId);
        Set<Long> patientIds = cps.stream().map(DiseaseCohortPatientEntity::getPatientId).collect(Collectors.toSet());
        Map<Long, PatientEntity> patientMap = patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(PatientEntity::getId, p -> p));

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + cohort.getName() + "_patients.csv");

        PrintWriter writer = response.getWriter();
        writer.write("\uFEFF"); // BOM for Excel
        writer.println("患者姓名,性别,年龄,匹配来源,匹配时间");
        for (DiseaseCohortPatientEntity cp : cps) {
            PatientEntity p = patientMap.get(cp.getPatientId());
            String name = p != null ? p.getName() : "-";
            String gender = p != null ? p.getGender() : "-";
            String age = (p != null && p.getBirthDate() != null)
                    ? String.valueOf(Period.between(p.getBirthDate(), java.time.LocalDate.now()).getYears()) : "-";
            writer.printf("%s,%s,%s,%s,%s%n", name, gender, age, cp.getMatchSource(), cp.getMatchedAt());
        }
        writer.flush();
    }
}
