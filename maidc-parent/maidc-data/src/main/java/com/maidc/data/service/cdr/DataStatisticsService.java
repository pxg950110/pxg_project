package com.maidc.data.service.cdr;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据中心统计服务（前端 DataDashboard 两图数据源）。
 * <p>直接对 cdr.* 做聚合，走 DDL 词汇（created_at/source_system），无 JPA 实体参与。
 */
@Service
@RequiredArgsConstructor
public class DataStatisticsService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 月度数据增长趋势。
     * <p>前端四条线的语义（DataDashboard series）：clinical=临床检验(c_lab_test)、
     * research=病历文书(c_clinical_note)、imaging=影像组学(c_imaging_exam)、
     * pathology=病理基因(c_pathology)。
     *
     * @param months 统计最近 N 个自然月（1-24，含当月）
     */
    public Map<String, Object> getDataGrowthTrend(int months) {
        int n = Math.min(Math.max(months, 1), 24);
        String sql = """
                SELECT to_char(m, 'YYYY-MM') AS month,
                       COUNT(DISTINCT lt.id)  AS clinical,
                       COUNT(DISTINCT cn.id)  AS research,
                       COUNT(DISTINCT ie.id)  AS imaging,
                       COUNT(DISTINCT pa.id)  AS pathology
                FROM generate_series(
                             date_trunc('month', CURRENT_DATE) - make_interval(months => ? - 1),
                             date_trunc('month', CURRENT_DATE),
                             interval '1 month') AS m
                LEFT JOIN cdr.c_lab_test lt
                       ON date_trunc('month', lt.created_at) = m AND lt.is_deleted = false
                LEFT JOIN cdr.c_clinical_note cn
                       ON date_trunc('month', cn.created_at) = m AND cn.is_deleted = false
                LEFT JOIN cdr.c_imaging_exam ie
                       ON date_trunc('month', ie.created_at) = m AND ie.is_deleted = false
                LEFT JOIN cdr.c_pathology pa
                       ON date_trunc('month', pa.created_at) = m AND pa.is_deleted = false
                GROUP BY m
                ORDER BY m
                """;
        List<String> monthKeys = new ArrayList<>();
        List<Long> clinical = new ArrayList<>();
        List<Long> research = new ArrayList<>();
        List<Long> imaging = new ArrayList<>();
        List<Long> pathology = new ArrayList<>();
        jdbcTemplate.query(sql, rs -> {
            monthKeys.add(rs.getString("month"));
            clinical.add(rs.getLong("clinical"));
            research.add(rs.getLong("research"));
            imaging.add(rs.getLong("imaging"));
            pathology.add(rs.getLong("pathology"));
        }, n);
        return Map.of(
                "months", monthKeys,
                "clinical", clinical,
                "research", research,
                "imaging", imaging,
                "pathology", pathology);
    }

    /**
     * 数据来源分布：按患者主索引的 source_system 聚合。
     * <p>返回 [{name, value}]，空值归并为 UNKNOWN，值为患者数。
     */
    public List<Map<String, Object>> getSourceDistribution() {
        String sql = """
                SELECT COALESCE(NULLIF(source_system, ''), 'UNKNOWN') AS name,
                       COUNT(*) AS value
                FROM cdr.c_patient
                WHERE is_deleted = false
                GROUP BY 1
                ORDER BY value DESC
                """;
        return jdbcTemplate.query(sql, (rs, i) -> Map.<String, Object>of(
                "name", rs.getString("name"),
                "value", rs.getLong("value")));
    }
}
