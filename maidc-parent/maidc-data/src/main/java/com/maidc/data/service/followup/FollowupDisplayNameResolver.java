package com.maidc.data.service.followup;

import com.maidc.data.entity.PatientFollowupEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 随访档案展示字段批量填充：患者（cdr.c_patient）与医护（system.s_user）名称。
 * 避免 N+1，一次 in 查询各表。
 */
@Component
@RequiredArgsConstructor
public class FollowupDisplayNameResolver {

    private final JdbcTemplate jdbcTemplate;

    public void fill(List<PatientFollowupEntity> followups) {
        if (followups == null || followups.isEmpty()) return;

        List<Long> patientIds = followups.stream().map(PatientFollowupEntity::getPatientId).distinct().toList();
        List<Long> userIds = followups.stream()
                .flatMap(f -> Stream.of(f.getDoctorId(), f.getNurseId()))
                .filter(Objects::nonNull).distinct().toList();

        Map<Long, Map<String, Object>> patients = loadPatients(patientIds);
        Map<Long, String> userNames = loadUserNames(userIds);

        LocalDate today = LocalDate.now();
        for (PatientFollowupEntity f : followups) {
            Map<String, Object> p = patients.get(f.getPatientId());
            if (p != null) {
                f.setPatientName((String) p.get("name"));
                f.setPatientGender((String) p.get("gender"));
                Object birth = p.get("birth_date");
                if (birth instanceof Date d) {
                    f.setPatientAge(Period.between(d.toLocalDate(), today).getYears());
                }
            }
            f.setDoctorName(userNames.get(f.getDoctorId()));
            f.setNurseName(userNames.get(f.getNurseId()));
        }
    }

    /** patientId → {id,name,gender,birth_date} */
    public Map<Long, Map<String, Object>> loadPatients(List<Long> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) return Map.of();
        String in = joinPlaceholders(patientIds.size());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name, gender, birth_date FROM cdr.c_patient WHERE id IN (" + in + ")",
                patientIds.toArray());
        return rows.stream().collect(Collectors.toMap(r -> ((Number) r.get("id")).longValue(), r -> r, (a, b) -> a));
    }

    /** userId → real_name */
    public Map<Long, String> loadUserNames(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        String in = joinPlaceholders(userIds.size());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, real_name FROM system.s_user WHERE id IN (" + in + ")",
                userIds.toArray());
        return rows.stream().collect(Collectors.toMap(
                r -> ((Number) r.get("id")).longValue(), r -> String.valueOf(r.get("real_name")), (a, b) -> a));
    }

    private String joinPlaceholders(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }
}
