package com.maidc.data.service.etl;

import com.maidc.data.entity.EtlFieldMappingEntity;
import com.maidc.data.entity.EtlStepEntity;
import com.maidc.data.repository.EtlFieldMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EtlConfigGenerator {

    private final EtlFieldMappingRepository fieldMappingRepository;

    private static final DateTimeFormatter SYNC_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generate Embulk YAML configuration from step definition and field mappings.
     */
    public String generateEmbulkConfig(EtlStepEntity step,
                                       String sourceHost, int sourcePort, String sourceDb,
                                       String sourceUser, String sourcePass,
                                       String targetHost, int targetPort, String targetDb,
                                       String targetUser, String targetPass) {

        List<EtlFieldMappingEntity> mappings =
                fieldMappingRepository.findByStepIdAndIsDeletedFalseOrderBySortOrder(step.getId());

        String selectClause = buildSelectClause(mappings);
        String whereClause = buildWhereClause(step);

        StringBuilder yaml = new StringBuilder();

        // in: section
        yaml.append("in:\n");
        yaml.append("  type: postgresql\n");
        yaml.append("  host: ").append(sourceHost).append("\n");
        yaml.append("  port: ").append(sourcePort).append("\n");
        yaml.append("  database: ").append(sourceDb).append("\n");
        yaml.append("  user: ").append(sourceUser).append("\n");
        yaml.append("  password: ").append(quote(sourcePass)).append("\n");

        String sourceTable = buildQualifiedTable(step.getSourceSchema(), step.getSourceTable());
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ").append(selectClause)
                .append(" FROM ").append(sourceTable);
        if (whereClause != null) {
            queryBuilder.append(" WHERE ").append(whereClause);
        }
        yaml.append("  query: \"").append(queryBuilder).append("\"\n");

        // out: section
        yaml.append("out:\n");
        yaml.append("  type: postgresql\n");
        yaml.append("  host: ").append(targetHost).append("\n");
        yaml.append("  port: ").append(targetPort).append("\n");
        yaml.append("  database: ").append(targetDb).append("\n");
        yaml.append("  user: ").append(targetUser).append("\n");
        yaml.append("  password: ").append(quote(targetPass)).append("\n");

        String targetTable = buildQualifiedTable(step.getTargetSchema(), step.getTargetTable());
        yaml.append("  table: ").append(targetTable).append("\n");
        yaml.append("  mode: merge\n");

        // column_options
        if (!mappings.isEmpty()) {
            yaml.append("  column_options:\n");
            for (EtlFieldMappingEntity mapping : mappings) {
                yaml.append("    ").append(mapping.getTargetColumn()).append(": ");
                if (isComputedTransform(mapping.getTransformType())) {
                    // SELECT 层已算出同名别名列，此处直通
                    yaml.append("{value_from: ").append(mapping.getTargetColumn()).append("}\n");
                } else if ("CONSTANT".equals(mapping.getTransformType())) {
                    yaml.append("{value: \"").append(escapeYaml(mapping.getDefaultValue())).append("\"}\n");
                } else {
                    // DIRECT and default: pass through as value_from
                    yaml.append("{value_from: ").append(mapping.getSourceColumn()).append("}\n");
                }
            }
        }

        String config = yaml.toString();
        log.debug("Generated Embulk config for step {}: {} chars", step.getId(), config.length());
        return config;
    }

    /**
     * Build comma-separated SELECT list from field mappings.
     * DIRECT/CONSTANT/未知类型保持既有直通行为；MAP/DATE_FMT/LOOKUP/EXPRESSION 在 SELECT 层
     * 生成受控 SQL 片段并以目标列名作为别名（Embulk column_options 相应 value_from 该别名）。
     */
    String buildSelectClause(List<EtlFieldMappingEntity> mappings) {
        return mappings.stream()
                .filter(m -> m.getSourceColumn() != null && !m.getSourceColumn().isBlank())
                .map(this::selectExpression)
                .collect(Collectors.joining(", "));
    }

    private String selectExpression(EtlFieldMappingEntity m) {
        String source = m.getSourceColumn();
        String type = m.getTransformType();
        if ("MAP".equals(type)) {
            return "CASE " + guardedExpr(m) + " END AS " + m.getTargetColumn();
        }
        if ("DATE_FMT".equals(type)) {
            String expr = m.getTransformExpr();
            if (expr == null || expr.isBlank()) {
                return "CAST(" + source + " AS VARCHAR) AS " + m.getTargetColumn();
            }
            return "TO_CHAR(" + source + ", " + guardedExpr(m) + ") AS " + m.getTargetColumn();
        }
        if ("LOOKUP".equals(type)) {
            return "(" + guardedExpr(m) + ") AS " + m.getTargetColumn();
        }
        if ("EXPRESSION".equals(type)) {
            return guardedExpr(m) + " AS " + m.getTargetColumn();
        }
        return source;
    }

    /**
     * transform_expr 由管理端受控录入，但仍拒绝会破坏查询语句的片段（语句终止符/行注释）。
     */
    private String guardedExpr(EtlFieldMappingEntity m) {
        String expr = m.getTransformExpr();
        if (expr == null || expr.isBlank()) {
            throw new IllegalArgumentException(
                    "transform_expr is required for " + m.getTransformType() + " on target column " + m.getTargetColumn());
        }
        String normalized = expr.trim();
        if (normalized.contains(";") || normalized.contains("--") || normalized.contains("/*")) {
            throw new IllegalArgumentException(
                    "transform_expr must not contain statement terminators or comments (target column " + m.getTargetColumn() + ")");
        }
        return normalized;
    }

    private boolean isComputedTransform(String type) {
        return "MAP".equals(type) || "DATE_FMT".equals(type) || "LOOKUP".equals(type) || "EXPRESSION".equals(type);
    }

    /**
     * Build WHERE clause from step configuration.
     * Returns null if no filter is applicable.
     */
    String buildWhereClause(EtlStepEntity step) {
        // Priority 1: explicit filter condition
        if (step.getFilterCondition() != null && !step.getFilterCondition().isBlank()) {
            return step.getFilterCondition();
        }

        // Priority 2: incremental sync based on lastSyncTime
        if ("INCREMENTAL".equals(step.getSyncMode()) && step.getLastSyncTime() != null) {
            return "_loaded_at > '" + step.getLastSyncTime().format(SYNC_TIME_FORMATTER) + "'";
        }

        return null;
    }

    private String buildQualifiedTable(String schema, String table) {
        if (schema != null && !schema.isBlank()) {
            return schema + "." + table;
        }
        return table;
    }

    private String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value + "\"";
    }

    private String escapeYaml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
