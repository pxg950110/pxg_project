package com.maidc.data.service;

import com.maidc.data.dto.*;
import com.maidc.data.entity.PatientEntity;
import com.maidc.data.repository.PatientRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartSearchService {

    private final EntityManager entityManager;
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

        String sql = buildUnionSql(domains);
        String countSql = buildCountSql(domains);

        // Count query
        Query countQuery = entityManager.createNativeQuery(countSql);
        countQuery.setParameter("keyword", req.getKeyword());
        // TODO: dateFrom/dateTo parameters added when buildDateFilter() is implemented
        long total = ((Number) countQuery.getSingleResult()).longValue();

        // Data query
        int offset = (req.getPage() - 1) * req.getPageSize();
        Query dataQuery = entityManager.createNativeQuery(sql);
        dataQuery.setParameter("keyword", req.getKeyword());
        // TODO: dateFrom/dateTo parameters added when buildDateFilter() is implemented
        dataQuery.setParameter("limit", req.getPageSize());
        dataQuery.setParameter("offset", offset);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();

        // Parse results
        Map<Long, String> patientNames = resolvePatientNames(rows);
        List<SmartSearchItem> items = rows.stream().map(row -> {
            SmartSearchItem item = new SmartSearchItem();
            item.setDomain((String) row[0]);
            item.setId(((Number) row[1]).longValue());
            Number pid = (Number) row[2];
            item.setPatientId(pid != null ? pid.longValue() : null);
            item.setTitle((String) row[3]);
            item.setSubtitle((String) row[4]);
            item.setScore(((Number) row[5]).doubleValue());
            item.setHeadline((String) row[6]);
            if (item.getPatientId() != null) {
                item.setPatientName(patientNames.getOrDefault(item.getPatientId(), "-"));
            }
            return item;
        }).collect(Collectors.toList());

        // Aggregations
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
        StringBuilder sb = new StringBuilder("SELECT COUNT(*) FROM (");
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
        StringBuilder sb = new StringBuilder("SELECT * FROM (");
        boolean first = true;
        for (String domain : domains) {
            if (!first) sb.append(" UNION ALL ");
            first = false;
            sb.append(domainSubSql(domain));
        }
        sb.append(") AS search_results ORDER BY score DESC LIMIT :limit OFFSET :offset");
        return sb.toString();
    }

    private String domainSubSql(String domain) {
        String dateFilter = buildDateFilter();
        return switch (domain) {
            case "PATIENT" -> """
                SELECT 'PATIENT' AS domain, p.id, p.id AS patient_id,
                       p.name AS title, p.gender || ' ' || p.birth_date AS subtitle,
                       ts_rank_cd(p.fts, query) AS score,
                       ts_headline('zh', coalesce(p.name,''), query) AS headline
                FROM cdr.c_patient p, plainto_tsquery('zh', :keyword) query
                WHERE p.fts @@ query AND p.is_deleted = false""" + dateFilter;
            case "ENCOUNTER" -> """
                SELECT 'ENCOUNTER' AS domain, e.id, e.patient_id,
                       coalesce(e.diagnosis_name, '-') AS title,
                       coalesce(e.dept_name,'') || ' ' || coalesce(e.doctor_name,'') AS subtitle,
                       ts_rank_cd(e.fts, query) AS score,
                       ts_headline('zh', coalesce(e.doctor_name,'') || ' ' || coalesce(e.diagnosis_name,'') || ' ' || coalesce(e.dept_name,''), query) AS headline
                FROM cdr.c_encounter e, plainto_tsquery('zh', :keyword) query
                WHERE e.fts @@ query AND e.is_deleted = false""" + dateFilter;
            case "DIAGNOSIS" -> """
                SELECT 'DIAGNOSIS' AS domain, d.id, d.patient_id,
                       d.icd_name AS title, d.icd_code AS subtitle,
                       ts_rank_cd(d.fts, query) AS score,
                       ts_headline('zh', coalesce(d.icd_name,'') || ' ' || coalesce(d.icd_code,''), query) AS headline
                FROM cdr.c_diagnosis d, plainto_tsquery('zh', :keyword) query
                WHERE d.fts @@ query AND d.is_deleted = false""" + dateFilter;
            case "LAB" -> """
                SELECT 'LAB' AS domain, l.id, l.patient_id,
                       l.test_name AS title, coalesce(l.specimen_type,'') || ' ' || coalesce(l.status,'') AS subtitle,
                       ts_rank_cd(l.fts, query) AS score,
                       ts_headline('zh', coalesce(l.test_name,'') || ' ' || coalesce(l.test_code,'') || ' ' || coalesce(l.ordering_doctor,''), query) AS headline
                FROM cdr.c_lab_test l, plainto_tsquery('zh', :keyword) query
                WHERE l.fts @@ query AND l.is_deleted = false""" + dateFilter;
            case "MEDICATION" -> """
                SELECT 'MEDICATION' AS domain, m.id, m.patient_id,
                       m.med_name AS title, coalesce(m.dosage,'') || ' ' || coalesce(m.route,'') || ' ' || coalesce(m.frequency,'') AS subtitle,
                       ts_rank_cd(m.fts, query) AS score,
                       ts_headline('zh', coalesce(m.med_name,'') || ' ' || coalesce(m.med_code,'') || ' ' || coalesce(m.prescriber,''), query) AS headline
                FROM cdr.c_medication m, plainto_tsquery('zh', :keyword) query
                WHERE m.fts @@ query AND m.is_deleted = false""" + dateFilter;
            case "IMAGING" -> """
                SELECT 'IMAGING' AS domain, i.id, i.patient_id,
                       i.exam_type AS title, coalesce(i.body_part,'') || ' ' || coalesce(i.modality,'') AS subtitle,
                       ts_rank_cd(i.fts, query) AS score,
                       ts_headline('zh', coalesce(i.exam_type,'') || ' ' || coalesce(i.body_part,'') || ' ' || coalesce(i.report_text,''), query) AS headline
                FROM cdr.c_imaging_exam i, plainto_tsquery('zh', :keyword) query
                WHERE i.fts @@ query AND i.is_deleted = false""" + dateFilter;
            case "SURGERY" -> """
                SELECT 'SURGERY' AS domain, o.id, o.patient_id,
                       o.operation_name AS title, coalesce(o.surgeon,'') || ' ' || coalesce(o.anesthesia_type,'') AS subtitle,
                       ts_rank_cd(o.fts, query) AS score,
                       ts_headline('zh', coalesce(o.operation_name,'') || ' ' || coalesce(o.operation_code,'') || ' ' || coalesce(o.surgeon,''), query) AS headline
                FROM cdr.c_operation o, plainto_tsquery('zh', :keyword) query
                WHERE o.fts @@ query AND o.is_deleted = false""" + dateFilter;
            case "PATHOLOGY" -> """
                SELECT 'PATHOLOGY' AS domain, p.id, p.patient_id,
                       p.diagnosis_desc AS title, coalesce(p.grade,'') || ' ' || coalesce(p.stage,'') AS subtitle,
                       ts_rank_cd(p.fts, query) AS score,
                       ts_headline('zh', coalesce(p.diagnosis_desc,'') || ' ' || coalesce(p.specimen_type,''), query) AS headline
                FROM cdr.c_pathology p, plainto_tsquery('zh', :keyword) query
                WHERE p.fts @@ query AND p.is_deleted = false""" + dateFilter;
            case "VITAL" -> """
                SELECT 'VITAL' AS domain, v.id, v.patient_id,
                       v.sign_type AS title, cast(v.sign_value as text) || ' ' || coalesce(v.unit,'') AS subtitle,
                       ts_rank_cd(v.fts, query) AS score,
                       ts_headline('zh', coalesce(v.sign_type,''), query) AS headline
                FROM cdr.c_vital_sign v, plainto_tsquery('zh', :keyword) query
                WHERE v.fts @@ query AND v.is_deleted = false""" + dateFilter;
            case "ALLERGY" -> """
                SELECT 'ALLERGY' AS domain, a.id, a.patient_id,
                       a.allergen AS title, coalesce(a.reaction,'') || ' ' || coalesce(a.severity,'') AS subtitle,
                       ts_rank_cd(a.fts, query) AS score,
                       ts_headline('zh', coalesce(a.allergen,'') || ' ' || coalesce(a.reaction,''), query) AS headline
                FROM cdr.c_allergy a, plainto_tsquery('zh', :keyword) query
                WHERE a.fts @@ query AND a.is_deleted = false""" + dateFilter;
            case "NOTE" -> """
                SELECT 'NOTE' AS domain, n.id, n.patient_id,
                       n.title AS title, coalesce(n.note_type,'') || ' ' || coalesce(n.author,'') AS subtitle,
                       ts_rank_cd(n.fts, query) AS score,
                       ts_headline('zh', coalesce(n.title,'') || ' ' || coalesce(n.content,'') || ' ' || coalesce(n.author,''), query) AS headline
                FROM cdr.c_clinical_note n, plainto_tsquery('zh', :keyword) query
                WHERE n.fts @@ query AND n.is_deleted = false""" + dateFilter;
            case "PROJECT" -> """
                SELECT 'PROJECT' AS domain, p.id, NULL::bigint AS patient_id,
                       p.project_name AS title, coalesce(p.description,'') AS subtitle,
                       ts_rank_cd(p.fts, query) AS score,
                       ts_headline('zh', coalesce(p.project_name,'') || ' ' || coalesce(p.description,''), query) AS headline
                FROM rdr.r_study_project p, plainto_tsquery('zh', :keyword) query
                WHERE p.fts @@ query AND p.is_deleted = false""" + dateFilter;
            case "DATASET" -> """
                SELECT 'DATASET' AS domain, d.id, NULL::bigint AS patient_id,
                       d.dataset_name AS title, coalesce(d.description,'') AS subtitle,
                       ts_rank_cd(d.fts, query) AS score,
                       ts_headline('zh', coalesce(d.dataset_name,'') || ' ' || coalesce(d.description,''), query) AS headline
                FROM rdr.r_dataset d, plainto_tsquery('zh', :keyword) query
                WHERE d.fts @@ query AND d.is_deleted = false""" + dateFilter;
            default -> "SELECT NULL::text, NULL::bigint, NULL::bigint, NULL::text, NULL::text, 0::float, NULL::text WHERE false";
        };
    }

    private String domainSubCountSql(String domain) {
        String dateFilter = buildDateFilter();
        return switch (domain) {
            case "PATIENT" -> "SELECT 1 FROM cdr.c_patient p, plainto_tsquery('zh', :keyword) query WHERE p.fts @@ query AND p.is_deleted = false" + dateFilter;
            case "ENCOUNTER" -> "SELECT 1 FROM cdr.c_encounter e, plainto_tsquery('zh', :keyword) query WHERE e.fts @@ query AND e.is_deleted = false" + dateFilter;
            case "DIAGNOSIS" -> "SELECT 1 FROM cdr.c_diagnosis d, plainto_tsquery('zh', :keyword) query WHERE d.fts @@ query AND d.is_deleted = false" + dateFilter;
            case "LAB" -> "SELECT 1 FROM cdr.c_lab_test l, plainto_tsquery('zh', :keyword) query WHERE l.fts @@ query AND l.is_deleted = false" + dateFilter;
            case "MEDICATION" -> "SELECT 1 FROM cdr.c_medication m, plainto_tsquery('zh', :keyword) query WHERE m.fts @@ query AND m.is_deleted = false" + dateFilter;
            case "IMAGING" -> "SELECT 1 FROM cdr.c_imaging_exam i, plainto_tsquery('zh', :keyword) query WHERE i.fts @@ query AND i.is_deleted = false" + dateFilter;
            case "SURGERY" -> "SELECT 1 FROM cdr.c_operation o, plainto_tsquery('zh', :keyword) query WHERE o.fts @@ query AND o.is_deleted = false" + dateFilter;
            case "PATHOLOGY" -> "SELECT 1 FROM cdr.c_pathology p, plainto_tsquery('zh', :keyword) query WHERE p.fts @@ query AND p.is_deleted = false" + dateFilter;
            case "VITAL" -> "SELECT 1 FROM cdr.c_vital_sign v, plainto_tsquery('zh', :keyword) query WHERE v.fts @@ query AND v.is_deleted = false" + dateFilter;
            case "ALLERGY" -> "SELECT 1 FROM cdr.c_allergy a, plainto_tsquery('zh', :keyword) query WHERE a.fts @@ query AND a.is_deleted = false" + dateFilter;
            case "NOTE" -> "SELECT 1 FROM cdr.c_clinical_note n, plainto_tsquery('zh', :keyword) query WHERE n.fts @@ query AND n.is_deleted = false" + dateFilter;
            case "PROJECT" -> "SELECT 1 FROM rdr.r_study_project p, plainto_tsquery('zh', :keyword) query WHERE p.fts @@ query AND p.is_deleted = false" + dateFilter;
            case "DATASET" -> "SELECT 1 FROM rdr.r_dataset d, plainto_tsquery('zh', :keyword) query WHERE d.fts @@ query AND d.is_deleted = false" + dateFilter;
            default -> "SELECT NULL WHERE false";
        };
    }

    private String buildDateFilter() {
        return "";
    }
}
