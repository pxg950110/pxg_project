package com.maidc.data.service;

import com.maidc.data.dto.*;
import com.maidc.data.entity.PatientEntity;
import com.maidc.data.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

import javax.sql.DataSource;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartSearchService {

    private final DataSource dataSource;
    private final PatientRepository patientRepository;

    private static final Set<String> ALL_DOMAINS = Set.of(
            "PATIENT", "ENCOUNTER", "DIAGNOSIS", "LAB", "MEDICATION",
            "IMAGING", "SURGERY", "PATHOLOGY", "VITAL", "ALLERGY", "NOTE",
            "PROJECT", "DATASET"
    );

    public SmartSearchResult search(SmartSearchRequest req) {
        Set<String> domains = (req.getDomains() == null || req.getDomains().isEmpty())
                ? ALL_DOMAINS
                : req.getDomains().stream().map(String::toUpperCase).filter(ALL_DOMAINS::contains).collect(Collectors.toSet());

        if (domains.isEmpty()) {
            SmartSearchResult empty = new SmartSearchResult();
            empty.setItems(List.of());
            empty.setTotal(0);
            empty.setPage(req.getPage());
            empty.setPageSize(req.getPageSize());
            empty.setAggregations(Map.of());
            return empty;
        }

        String keyword = req.getKeyword();
        int offset = (req.getPage() - 1) * req.getPageSize();
        int limit = req.getPageSize();

        long total = 0;
        List<Object[]> rows = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            // Count
            String countSql = buildCountSql(domains);
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                ps.setString(1, keyword);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) total = rs.getLong(1);
                }
            }

            // Data
            String dataSql = buildUnionSql(domains);
            try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                ps.setString(1, keyword);
                ps.setInt(2, limit);
                ps.setInt(3, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Object patientId = rs.getObject("patient_id");
                        rows.add(new Object[]{
                                rs.getString("domain"),
                                rs.getLong("id"),
                                patientId != null ? ((Number) patientId).longValue() : null,
                                rs.getString("title"),
                                rs.getString("subtitle"),
                                rs.getDouble("score"),
                                rs.getString("headline")
                        });
                    }
                }
            }
        } catch (Exception e) {
            log.error("Smart search failed", e);
            SmartSearchResult err = new SmartSearchResult();
            err.setItems(List.of());
            err.setTotal(0);
            err.setPage(req.getPage());
            err.setPageSize(req.getPageSize());
            err.setAggregations(Map.of());
            return err;
        }

        Map<Long, String> patientNames = resolvePatientNames(rows);
        List<SmartSearchItem> items = rows.stream().map(row -> {
            SmartSearchItem item = new SmartSearchItem();
            item.setDomain((String) row[0]);
            item.setId(((Number) row[1]).longValue());
            item.setPatientId((Long) row[2]);
            item.setTitle((String) row[3]);
            item.setSubtitle((String) row[4]);
            item.setScore(((Number) row[5]).doubleValue());
            item.setHeadline((String) row[6]);
            if (item.getPatientId() != null) {
                item.setPatientName(patientNames.getOrDefault(item.getPatientId(), "-"));
            }
            return item;
        }).collect(Collectors.toList());

        Map<String, Long> aggregations = items.stream()
                .collect(Collectors.groupingBy(SmartSearchItem::getDomain, Collectors.counting()));

        SmartSearchResult result = new SmartSearchResult();
        result.setItems(items);
        result.setTotal(total);
        result.setPage(req.getPage());
        result.setPageSize(req.getPageSize());
        result.setAggregations(aggregations);
        return result;
    }

    private Map<Long, String> resolvePatientNames(List<Object[]> rows) {
        Set<Long> patientIds = rows.stream()
                .map(row -> (Number) row[2])
                .filter(Objects::nonNull)
                .map(Number::longValue)
                .collect(Collectors.toSet());
        if (patientIds.isEmpty()) return Map.of();
        return patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(PatientEntity::getId, PatientEntity::getName));
    }

    private String buildCountSql(Set<String> domains) {
        StringBuilder sb = new StringBuilder();
        sb.append("WITH tsq AS (SELECT plainto_tsquery('simple', ?) AS q) ");
        sb.append("SELECT COUNT(*) FROM (");
        boolean first = true;
        for (String domain : domains) {
            if (!first) sb.append(" UNION ALL ");
            first = false;
            sb.append(domainSubCountSql(domain));
        }
        sb.append(") AS total");
        return sb.toString();
    }

    private String buildUnionSql(Set<String> domains) {
        StringBuilder sb = new StringBuilder();
        sb.append("WITH tsq AS (SELECT plainto_tsquery('simple', ?) AS q) ");
        sb.append("SELECT * FROM (");
        boolean first = true;
        for (String domain : domains) {
            if (!first) sb.append(" UNION ALL ");
            first = false;
            sb.append(domainSubSql(domain));
        }
        sb.append(") AS search_results ORDER BY score DESC LIMIT ? OFFSET ?");
        return sb.toString();
    }

    private String domainSubSql(String domain) {
        return switch (domain) {
            case "PATIENT" -> """
                SELECT 'PATIENT' AS domain, p.id, p.id AS patient_id,
                       p.name AS title, p.gender || ' ' || p.birth_date AS subtitle,
                       ts_rank_cd(p.fts, q) AS score,
                       ts_headline('simple', coalesce(p.name,''), q) AS headline
                FROM cdr.c_patient p, tsq
                WHERE p.fts @@ q AND p.is_deleted = false""";
            case "ENCOUNTER" -> """
                SELECT 'ENCOUNTER' AS domain, e.id, e.patient_id,
                       coalesce(e.diagnosis_summary, '-') AS title,
                       coalesce(e.department,'') || ' ' || coalesce(e.attending_doctor,'') AS subtitle,
                       ts_rank_cd(e.fts, q) AS score,
                       ts_headline('simple', coalesce(e.attending_doctor,'') || ' ' || coalesce(e.diagnosis_summary,'') || ' ' || coalesce(e.department,''), q) AS headline
                FROM cdr.c_encounter e, tsq
                WHERE e.fts @@ q AND e.is_deleted = false""";
            case "DIAGNOSIS" -> """
                SELECT 'DIAGNOSIS' AS domain, d.id, d.patient_id,
                       d.diagnosis_name AS title, d.diagnosis_code AS subtitle,
                       ts_rank_cd(d.fts, q) AS score,
                       ts_headline('simple', coalesce(d.diagnosis_name,'') || ' ' || coalesce(d.diagnosis_code,''), q) AS headline
                FROM cdr.c_diagnosis d, tsq
                WHERE d.fts @@ q AND d.is_deleted = false""";
            case "LAB" -> """
                SELECT 'LAB' AS domain, l.id, l.patient_id,
                       l.test_name AS title, coalesce(l.specimen_type,'') || ' ' || coalesce(l.status,'') AS subtitle,
                       ts_rank_cd(l.fts, q) AS score,
                       ts_headline('simple', coalesce(l.test_name,'') || ' ' || coalesce(l.test_code,'') || ' ' || coalesce(l.ordering_doctor,''), q) AS headline
                FROM cdr.c_lab_test l, tsq
                WHERE l.fts @@ q AND l.is_deleted = false""";
            case "MEDICATION" -> """
                SELECT 'MEDICATION' AS domain, m.id, m.patient_id,
                       m.med_name AS title, coalesce(m.dosage,'') || ' ' || coalesce(m.route,'') || ' ' || coalesce(m.frequency,'') AS subtitle,
                       ts_rank_cd(m.fts, q) AS score,
                       ts_headline('simple', coalesce(m.med_name,'') || ' ' || coalesce(m.med_code,'') || ' ' || coalesce(m.prescriber,''), q) AS headline
                FROM cdr.c_medication m, tsq
                WHERE m.fts @@ q AND m.is_deleted = false""";
            case "IMAGING" -> """
                SELECT 'IMAGING' AS domain, i.id, i.patient_id,
                       i.exam_type AS title, coalesce(i.body_part,'') || ' ' || coalesce(i.modality,'') AS subtitle,
                       ts_rank_cd(i.fts, q) AS score,
                       ts_headline('simple', coalesce(i.exam_type,'') || ' ' || coalesce(i.body_part,'') || ' ' || coalesce(i.report_text,''), q) AS headline
                FROM cdr.c_imaging_exam i, tsq
                WHERE i.fts @@ q AND i.is_deleted = false""";
            case "SURGERY" -> """
                SELECT 'SURGERY' AS domain, o.id, o.patient_id,
                       o.operation_name AS title, coalesce(o.surgeon,'') || ' ' || coalesce(o.anesthesia_type,'') AS subtitle,
                       ts_rank_cd(o.fts, q) AS score,
                       ts_headline('simple', coalesce(o.operation_name,'') || ' ' || coalesce(o.operation_code,'') || ' ' || coalesce(o.surgeon,''), q) AS headline
                FROM cdr.c_operation o, tsq
                WHERE o.fts @@ q AND o.is_deleted = false""";
            case "PATHOLOGY" -> """
                SELECT 'PATHOLOGY' AS domain, p.id, p.patient_id,
                       p.diagnosis_desc AS title, coalesce(p.grade,'') || ' ' || coalesce(p.stage,'') AS subtitle,
                       ts_rank_cd(p.fts, q) AS score,
                       ts_headline('simple', coalesce(p.diagnosis_desc,'') || ' ' || coalesce(p.specimen_type,''), q) AS headline
                FROM cdr.c_pathology p, tsq
                WHERE p.fts @@ q AND p.is_deleted = false""";
            case "VITAL" -> """
                SELECT 'VITAL' AS domain, v.id, v.patient_id,
                       v.sign_type AS title, cast(v.sign_value as text) || ' ' || coalesce(v.unit,'') AS subtitle,
                       ts_rank_cd(v.fts, q) AS score,
                       ts_headline('simple', coalesce(v.sign_type,''), q) AS headline
                FROM cdr.c_vital_sign v, tsq
                WHERE v.fts @@ q AND v.is_deleted = false""";
            case "ALLERGY" -> """
                SELECT 'ALLERGY' AS domain, a.id, a.patient_id,
                       a.allergen AS title, coalesce(a.reaction,'') || ' ' || coalesce(a.severity,'') AS subtitle,
                       ts_rank_cd(a.fts, q) AS score,
                       ts_headline('simple', coalesce(a.allergen,'') || ' ' || coalesce(a.reaction,''), q) AS headline
                FROM cdr.c_allergy a, tsq
                WHERE a.fts @@ q AND a.is_deleted = false""";
            case "NOTE" -> """
                SELECT 'NOTE' AS domain, n.id, n.patient_id,
                       n.title AS title, coalesce(n.note_type,'') || ' ' || coalesce(n.author,'') AS subtitle,
                       ts_rank_cd(n.fts, q) AS score,
                       ts_headline('simple', coalesce(n.title,'') || ' ' || coalesce(n.content,'') || ' ' || coalesce(n.author,''), q) AS headline
                FROM cdr.c_clinical_note n, tsq
                WHERE n.fts @@ q AND n.is_deleted = false""";
            case "PROJECT" -> """
                SELECT 'PROJECT' AS domain, p.id, NULL::bigint AS patient_id,
                       p.name AS title, coalesce(p.description,'') AS subtitle,
                       ts_rank_cd(p.fts, q) AS score,
                       ts_headline('simple', coalesce(p.name,'') || ' ' || coalesce(p.description,''), q) AS headline
                FROM rdr.r_study_project p, tsq
                WHERE p.fts @@ q AND p.is_deleted = false""";
            case "DATASET" -> """
                SELECT 'DATASET' AS domain, d.id, NULL::bigint AS patient_id,
                       d.name AS title, coalesce(d.description,'') AS subtitle,
                       ts_rank_cd(d.fts, q) AS score,
                       ts_headline('simple', coalesce(d.name,'') || ' ' || coalesce(d.description,''), q) AS headline
                FROM rdr.r_dataset d, tsq
                WHERE d.fts @@ q AND d.is_deleted = false""";
            default -> "SELECT NULL::text AS domain, NULL::bigint AS id, NULL::bigint AS patient_id, NULL::text AS title, NULL::text AS subtitle, 0::float AS score, NULL::text AS headline WHERE false";
        };
    }

    private String domainSubCountSql(String domain) {
        return switch (domain) {
            case "PATIENT" -> "SELECT 1 FROM cdr.c_patient p, tsq WHERE p.fts @@ q AND p.is_deleted = false";
            case "ENCOUNTER" -> "SELECT 1 FROM cdr.c_encounter e, tsq WHERE e.fts @@ q AND e.is_deleted = false";
            case "DIAGNOSIS" -> "SELECT 1 FROM cdr.c_diagnosis d, tsq WHERE d.fts @@ q AND d.is_deleted = false";
            case "LAB" -> "SELECT 1 FROM cdr.c_lab_test l, tsq WHERE l.fts @@ q AND l.is_deleted = false";
            case "MEDICATION" -> "SELECT 1 FROM cdr.c_medication m, tsq WHERE m.fts @@ q AND m.is_deleted = false";
            case "IMAGING" -> "SELECT 1 FROM cdr.c_imaging_exam i, tsq WHERE i.fts @@ q AND i.is_deleted = false";
            case "SURGERY" -> "SELECT 1 FROM cdr.c_operation o, tsq WHERE o.fts @@ q AND o.is_deleted = false";
            case "PATHOLOGY" -> "SELECT 1 FROM cdr.c_pathology p, tsq WHERE p.fts @@ q AND p.is_deleted = false";
            case "VITAL" -> "SELECT 1 FROM cdr.c_vital_sign v, tsq WHERE v.fts @@ q AND v.is_deleted = false";
            case "ALLERGY" -> "SELECT 1 FROM cdr.c_allergy a, tsq WHERE a.fts @@ q AND a.is_deleted = false";
            case "NOTE" -> "SELECT 1 FROM cdr.c_clinical_note n, tsq WHERE n.fts @@ q AND n.is_deleted = false";
            case "PROJECT" -> "SELECT 1 FROM rdr.r_study_project p, tsq WHERE p.fts @@ q AND p.is_deleted = false";
            case "DATASET" -> "SELECT 1 FROM rdr.r_dataset d, tsq WHERE d.fts @@ q AND d.is_deleted = false";
            default -> "SELECT NULL WHERE false";
        };
    }
}
