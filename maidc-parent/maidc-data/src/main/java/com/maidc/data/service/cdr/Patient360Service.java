package com.maidc.data.service.cdr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CDR患者360视图服务
 * 提供患者综合数据查询和聚合功能
 */
@Slf4j
// 与 com.maidc.data.service.Patient360Service 类名相同，需显式 bean 名避免冲突
@Service("cdrPatient360Service")
@RequiredArgsConstructor
public class Patient360Service {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取患者基本信息
     */
    public Map<String, Object> getPatientBasicInfo(Long patientId) {
        String sql = """
            SELECT p.patient_no, p.name, p.gender, p.birth_date, p.blood_type,
                   p.phone, p.id_card_no, p.address, p.source_system,
                   COALESCE(p.anchor_age, DATE_PART('year', AGE(CURRENT_DATE, p.birth_date))::int) AS age,
                   p.dod
            FROM cdr.c_patient p
            WHERE p.id = ? AND p.is_deleted = false
            """;
        try {
            return jdbcTemplate.queryForMap(sql, patientId);
        } catch (Exception e) {
            log.warn("患者基本信息查询失败: patientId={}", patientId);
            return new HashMap<>();
        }
    }

    /**
     * 获取患者就诊统计
     */
    public Map<String, Object> getEncounterStats(Long patientId) {
        String sql = """
            SELECT COUNT(*) as total_encounters,
                   COUNT(*) FILTER (WHERE encounter_type = 'INPATIENT') as inpatient_count,
                   COUNT(*) FILTER (WHERE encounter_type = 'OUTPATIENT') as outpatient_count,
                   COUNT(*) FILTER (WHERE encounter_type = 'EMERGENCY') as emergency_count,
                   COALESCE(SUM(EXTRACT(EPOCH FROM (discharge_time - admit_time)) / 86400)
                       FILTER (WHERE discharge_time IS NOT NULL AND admit_time IS NOT NULL), 0)::int as total_los_days,
                   MIN(admit_time) as first_encounter,
                   MAX(admit_time) as last_encounter
            FROM cdr.c_encounter
            WHERE patient_id = ? AND is_deleted = false
            """;
        try {
            return jdbcTemplate.queryForMap(sql, patientId);
        } catch (Exception e) {
            log.warn("就诊统计查询失败: patientId={}", patientId);
            return new HashMap<>();
        }
    }

    /**
     * 获取患者诊断统计
     */
    public Map<String, Object> getDiagnosisStats(Long patientId) {
        String sql = """
            SELECT COUNT(DISTINCT icd_code) as diagnosis_types,
                   COUNT(*) FILTER (WHERE diagnosis_type = 'MAIN') as primary_diagnosis,
                   STRING_AGG(DISTINCT icd_code, ',') as all_diag_codes
            FROM cdr.c_diagnosis
            WHERE patient_id = ? AND is_deleted = false
            """;
        try {
            return jdbcTemplate.queryForMap(sql, patientId);
        } catch (Exception e) {
            log.warn("诊断统计查询失败: patientId={}", patientId);
            return new HashMap<>();
        }
    }

    /**
     * 获取患者检验统计
     */
    public Map<String, Object> getLabStats(Long patientId) {
        String sql = """
            SELECT COUNT(DISTINCT t.test_code) as lab_test_types,
                   COUNT(*) as total_tests,
                   COUNT(*) FILTER (WHERE EXISTS (
                       SELECT 1 FROM cdr.c_lab_panel pn
                       WHERE pn.test_id = t.id AND pn.is_deleted = false AND pn.abnormal_flag = true)) as abnormal_count,
                   MAX(COALESCE(t.reported_at, t.collected_at, t.ordered_at)) as latest_lab_time
            FROM cdr.c_lab_test t
            WHERE t.patient_id = ? AND t.is_deleted = false
            """;
        try {
            return jdbcTemplate.queryForMap(sql, patientId);
        } catch (Exception e) {
            log.warn("检验统计查询失败: patientId={}", patientId);
            return new HashMap<>();
        }
    }

    /**
     * 获取患者影像统计
     */
    public Map<String, Object> getImagingStats(Long patientId) {
        String sql = """
            SELECT COUNT(*) as total_imaging,
                   COUNT(*) FILTER (WHERE exam_type = 'CT') as ct_count,
                   COUNT(*) FILTER (WHERE exam_type = 'MRI') as mri_count,
                   COUNT(*) FILTER (WHERE exam_type = 'XRAY') as xray_count,
                   COUNT(*) FILTER (WHERE exam_type = 'ULTRASOUND') as ultrasound_count,
                   MAX(study_date) as latest_imaging_time
            FROM cdr.c_imaging_exam
            WHERE patient_id = ? AND is_deleted = false
            """;
        try {
            return jdbcTemplate.queryForMap(sql, patientId);
        } catch (Exception e) {
            log.warn("影像统计查询失败: patientId={}", patientId);
            return new HashMap<>();
        }
    }

    /**
     * 获取患者过敏史
     */
    public List<Map<String, Object>> getAllergies(Long patientId) {
        // 前端契约键 onset_date 由 DDL 确认时间列 confirmed_at 供数
        String sql = """
            SELECT allergen, allergen_type, reaction, severity,
                   confirmed_at AS onset_date
            FROM cdr.c_allergy
            WHERE patient_id = ? AND is_deleted = false
            ORDER BY confirmed_at DESC NULLS LAST
            """;
        try {
            return jdbcTemplate.queryForList(sql, patientId);
        } catch (Exception e) {
            log.warn("过敏史查询失败: patientId={}", patientId);
            return new ArrayList<>();
        }
    }

    /**
     * 获取患者家族史
     */
    public List<Map<String, Object>> getFamilyHistory(Long patientId) {
        // 前端契约键 relation/disease 由 DDL 列 relationship/disease_name 供数
        String sql = """
            SELECT relationship AS relation, disease_name AS disease,
                   icd_code, onset_age
            FROM cdr.c_family_history
            WHERE patient_id = ? AND is_deleted = false
            """;
        try {
            return jdbcTemplate.queryForList(sql, patientId);
        } catch (Exception e) {
            log.warn("家族史查询失败: patientId={}", patientId);
            return new ArrayList<>();
        }
    }

    /**
     * 获取患者就诊时间轴
     */
    public List<Map<String, Object>> getTimeline(Long patientId, LocalDate startDate, LocalDate endDate) {
        // 前端契约键 admission_time/department/attending_doctor 由 DDL 列 admit_time/dept_name/doctor_name 别名供数
        String sql = """
            SELECT e.id as encounter_id, e.admit_time AS admission_time, e.discharge_time, e.encounter_type,
                   e.dept_name AS department, e.doctor_name AS attending_doctor,
                   d.icd_code, d.icd_name, d.diagnosis_type
            FROM cdr.c_encounter e
            LEFT JOIN cdr.c_diagnosis d ON e.id = d.encounter_id AND d.is_deleted = false
            WHERE e.patient_id = ? AND e.is_deleted = false
            AND e.admit_time BETWEEN ? AND ?
            ORDER BY e.admit_time DESC
            """;
        try {
            return jdbcTemplate.queryForList(sql, patientId, startDate, endDate);
        } catch (Exception e) {
            log.warn("就诊时间轴查询失败: patientId={}", patientId);
            return new ArrayList<>();
        }
    }

    /**
     * 获取患者完整360视图数据
     */
    public Map<String, Object> getPatient360(Long patientId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("basicInfo", getPatientBasicInfo(patientId));
        result.put("encounterStats", getEncounterStats(patientId));
        result.put("diagnosisStats", getDiagnosisStats(patientId));
        result.put("labStats", getLabStats(patientId));
        result.put("imagingStats", getImagingStats(patientId));
        result.put("allergies", getAllergies(patientId));
        result.put("familyHistory", getFamilyHistory(patientId));
        return result;
    }

    /**
     * 计算患者数据完整性得分
     */
    public BigDecimal calculateCompletenessScore(Long patientId) {
        int score = 100;
        int missingFields = 0;

        Map<String, Object> basic = getPatientBasicInfo(patientId);
        if (basic.isEmpty()) return BigDecimal.ZERO;

        // 检查必填字段
        if (basic.get("gender") == null) missingFields += 5;
        if (basic.get("birth_date") == null) missingFields += 5;
        if (basic.get("phone") == null) missingFields += 3;

        // 检查关键数据存在性
        Map<String, Object> encounters = getEncounterStats(patientId);
        if (encounters.isEmpty() || ((Number) encounters.getOrDefault("total_encounters", 0)).intValue() == 0) {
            missingFields += 10;
        }

        Map<String, Object> diagnoses = getDiagnosisStats(patientId);
        if (diagnoses.isEmpty() || ((Number) diagnoses.getOrDefault("diagnosis_types", 0)).intValue() == 0) {
            missingFields += 8;
        }

        score = Math.max(0, score - missingFields);
        return BigDecimal.valueOf(score);
    }

    /**
     * 诊断地图：诊断按 ICD 章节聚合到身体系统，附人体热区坐标（百分比）
     */
    public List<Map<String, Object>> getDiagnosisMap(Long patientId) {
        String sql = """
            SELECT d.icd_code AS code,
                   d.icd_name AS name,
                   COUNT(DISTINCT d.encounter_id) AS visit_count,
                   MAX(e.admit_time) AS last_visit,
                   BOOL_OR(d.diagnosis_type = 'MAIN') AS is_primary
            FROM cdr.c_diagnosis d
            LEFT JOIN cdr.c_encounter e ON e.id = d.encounter_id AND e.is_deleted = false
            WHERE d.patient_id = ? AND d.is_deleted = false
            GROUP BY 1, 2
            ORDER BY visit_count DESC, last_visit DESC
            LIMIT 12
            """;
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(sql, patientId);
        } catch (Exception e) {
            log.warn("诊断地图查询失败: patientId={}", patientId);
            return new ArrayList<>();
        }

        Map<String, Map<String, Object>> systems = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String code = row.get("code") == null ? "" : String.valueOf(row.get("code"));
            BodySystem bs = resolveBodySystem(code);
            Map<String, Object> system = systems.computeIfAbsent(bs.system(), k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("system", bs.system());
                if (bs.hotspotX() != null) {
                    m.put("hotspotX", bs.hotspotX());
                    m.put("hotspotY", bs.hotspotY());
                }
                m.put("diagnoses", new ArrayList<Map<String, Object>>());
                return m;
            });
            Map<String, Object> diag = new HashMap<>();
            diag.put("code", code);
            diag.put("name", row.get("name"));
            diag.put("visitCount", row.get("visit_count") == null ? 0 : ((Number) row.get("visit_count")).intValue());
            diag.put("lastVisit", row.get("last_visit") == null ? null : String.valueOf(row.get("last_visit")));
            diag.put("isPrimary", Boolean.TRUE.equals(row.get("is_primary")));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> diagnoses = (List<Map<String, Object>>) system.get("diagnoses");
            diagnoses.add(diag);
        }
        return new ArrayList<>(systems.values());
    }

    /** 最近异常体检指标（c_checkup_item_result.abnormal_flag），direction 由结果值对比参考范围推导 */
    public Map<String, Object> getRecentAbnormalIndicators(Long patientId, int limit) {
        String totalSql = """
            SELECT COUNT(*) FROM cdr.c_checkup_item_result ir
            JOIN cdr.c_checkup_package pkg ON pkg.id = ir.package_id AND pkg.is_deleted = false
            JOIN cdr.c_health_checkup hc ON hc.id = pkg.checkup_id AND hc.is_deleted = false
            WHERE hc.patient_id = ? AND ir.is_deleted = false AND ir.abnormal_flag = true
            """;
        String itemsSql = """
            SELECT ir.item_name, ir.result_value, ir.unit, ir.reference_range,
                   hc.checkup_date
            FROM cdr.c_checkup_item_result ir
            JOIN cdr.c_checkup_package pkg ON pkg.id = ir.package_id AND pkg.is_deleted = false
            JOIN cdr.c_health_checkup hc ON hc.id = pkg.checkup_id AND hc.is_deleted = false
            WHERE hc.patient_id = ? AND ir.is_deleted = false AND ir.abnormal_flag = true
            ORDER BY hc.checkup_date DESC, ir.id DESC
            LIMIT ?
            """;
        Map<String, Object> result = new HashMap<>();
        result.put("total", 0L);
        result.put("items", new ArrayList<>());
        try {
            Long total = jdbcTemplate.queryForObject(totalSql, Long.class, patientId);
            result.put("total", total == null ? 0L : total);
            List<Map<String, Object>> items = jdbcTemplate.queryForList(itemsSql, patientId, limit);
            for (Map<String, Object> item : items) {
                item.put("direction", directionOf(
                        item.get("result_value") == null ? null : String.valueOf(item.get("result_value")),
                        item.get("reference_range") == null ? null : String.valueOf(item.get("reference_range"))));
            }
            result.put("items", items);
        } catch (Exception e) {
            log.warn("异常指标查询失败: patientId={}", patientId);
        }
        return result;
    }

    private String directionOf(String value, String range) {
        if (value == null || value.isBlank()) {
            return "异常";
        }
        try {
            double v = Double.parseDouble(value.replaceAll("[^0-9.\\-]", ""));
            if (range != null) {
                var rangeMatcher = java.util.regex.Pattern
                        .compile("^(-?\\d+(?:\\.\\d+)?)\\s*[-~至]{1,2}\\s*(-?\\d+(?:\\.\\d+)?)")
                        .matcher(range.trim());
                if (rangeMatcher.find()) {
                    if (v > Double.parseDouble(rangeMatcher.group(2))) return "偏高";
                    if (v < Double.parseDouble(rangeMatcher.group(1))) return "偏低";
                    return "异常";
                }
                var boundMatcher = java.util.regex.Pattern
                        .compile("^([<>])\\s*(-?\\d+(?:\\.\\d+)?)")
                        .matcher(range.trim());
                if (boundMatcher.find()) {
                    double bound = Double.parseDouble(boundMatcher.group(2));
                    if (">".equals(boundMatcher.group(1)) && v < bound) return "偏低";
                    if ("<".equals(boundMatcher.group(1)) && v > bound) return "偏高";
                }
            }
        } catch (NumberFormatException ignored) {
            // 非数值型结果（如"阳性"）保持"异常"
        }
        return "异常";
    }

    /**
     * ICD-10 首字母 → 身体系统与人体热区（百分比坐标，与前端人体 SVG 对应）
     */
    record BodySystem(String system, Double hotspotX, Double hotspotY) {
    }

    static BodySystem resolveBodySystem(String icdCode) {
        char ch = icdCode == null || icdCode.isEmpty() ? '?' : Character.toUpperCase(icdCode.charAt(0));
        return switch (ch) {
            case 'A', 'B' -> new BodySystem("感染与免疫", 50d, 38d);
            case 'C', 'D' -> new BodySystem("肿瘤", 50d, 36d);
            case 'E' -> new BodySystem("内分泌与代谢", 52d, 51d);
            case 'F' -> new BodySystem("精神心理", 50d, 6d);
            case 'G' -> new BodySystem("神经系统", 50d, 7d);
            case 'H' -> new BodySystem("感觉器官", 50d, 11d);
            case 'I' -> new BodySystem("循环系统", 43d, 30d);
            case 'J' -> new BodySystem("呼吸系统", 50d, 15d);
            case 'K' -> new BodySystem("消化系统", 47d, 43d);
            case 'L' -> new BodySystem("皮肤", 50d, 35d);
            case 'M' -> new BodySystem("肌肉骨骼", 45d, 80d);
            case 'N' -> new BodySystem("泌尿生殖", 52d, 62d);
            case 'R' -> new BodySystem("症状与体征", 50d, 35d);
            case 'S', 'T' -> new BodySystem("损伤与中毒", 45d, 80d);
            case 'Z' -> new BodySystem("健康状态影响", 50d, 35d);
            default -> new BodySystem("其他", null, null);
        };
    }

    /**
     * 搜索患者
     */
    public List<Map<String, Object>> searchPatients(String gender, Integer minAge, Integer maxAge,
                                                      String diagCodes, String encounterType,
                                                      LocalDate startTime, LocalDate endTime,
                                                      int page, int size) {
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT p.id, p.patient_no, p.name, p.gender, p.birth_date,
                   COALESCE(p.anchor_age, DATE_PART('year', AGE(CURRENT_DATE, p.birth_date))::int) AS age,
                   (SELECT COUNT(*) FROM cdr.c_encounter e WHERE e.patient_id = p.id AND e.is_deleted = false) as encounter_count,
                   (SELECT COUNT(DISTINCT d.icd_code) FROM cdr.c_diagnosis d WHERE d.patient_id = p.id AND d.is_deleted = false) as diag_count
            FROM cdr.c_patient p
            """);

        List<Object> params = new ArrayList<>();
        boolean hasWhere = false;

        if (gender != null && !gender.isBlank()) {
            sql.append(hasWhere ? " AND" : " WHERE").append(" p.gender = ?");
            params.add(gender);
            hasWhere = true;
        }
        // c_patient 无 age 列，年龄由 anchor_age/birth_date 表达式推导（别名不可用于 WHERE，需重复表达式）
        if (minAge != null) {
            sql.append(hasWhere ? " AND" : " WHERE")
               .append(" COALESCE(p.anchor_age, DATE_PART('year', AGE(CURRENT_DATE, p.birth_date))::int) >= ?");
            params.add(minAge);
            hasWhere = true;
        }
        if (maxAge != null) {
            sql.append(hasWhere ? " AND" : " WHERE")
               .append(" COALESCE(p.anchor_age, DATE_PART('year', AGE(CURRENT_DATE, p.birth_date))::int) <= ?");
            params.add(maxAge);
            hasWhere = true;
        }
        if (diagCodes != null && !diagCodes.isBlank()) {
            sql.append(hasWhere ? " AND" : " WHERE").append(" EXISTS (SELECT 1 FROM cdr.c_diagnosis d WHERE d.patient_id = p.id AND d.is_deleted = false AND d.icd_code IN (SELECT unnest(string_to_array(?, ','))))");
            params.add(diagCodes);
            hasWhere = true;
        }
        if (encounterType != null && !encounterType.isBlank()) {
            sql.append(hasWhere ? " AND" : " WHERE").append(" EXISTS (SELECT 1 FROM cdr.c_encounter e2 WHERE e2.patient_id = p.id AND e2.is_deleted = false AND e2.encounter_type = ?)");
            params.add(encounterType);
            hasWhere = true;
        }
        if (startTime != null) {
            sql.append(hasWhere ? " AND" : " WHERE").append(" EXISTS (SELECT 1 FROM cdr.c_encounter e3 WHERE e3.patient_id = p.id AND e3.is_deleted = false AND e3.admit_time >= ?)");
            params.add(startTime.atStartOfDay());
            hasWhere = true;
        }
        if (endTime != null) {
            sql.append(hasWhere ? " AND" : " WHERE").append(" EXISTS (SELECT 1 FROM cdr.c_encounter e4 WHERE e4.patient_id = p.id AND e4.is_deleted = false AND e4.admit_time < ?)");
            params.add(endTime.plusDays(1).atStartOfDay());
            hasWhere = true;
        }

        sql.append(" ORDER BY p.id LIMIT ? OFFSET ?");
        params.add(size);
        params.add((page - 1) * size);

        try {
            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
        } catch (Exception e) {
            log.error("患者搜索失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}