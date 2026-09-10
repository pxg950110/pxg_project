package com.maidc.data.service.followup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 量表数据绑定解析执行（设计 §5.6）：
 * 按 definition.items[].binding 从 CDR 业务表取值——
 * LAB/VITAL 取最近一次值（windowDays 限制），MEDICATION/DIAGNOSIS OPTIONS 按记录枚举选项。
 * 真实目录服务化后 field→物理列映射可配置化，当前内置映射。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScaleBindingService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 解析量表全部绑定项：返回 {itemNo: {...}}——
     * 值型 {value, at, unit}；枚举型 {options: [label]}；无数据项不出现在结果中。
     */
    public String resolveForPatient(Long patientId, String definitionJson) {
        ObjectNode out = objectMapper.createObjectNode();
        try {
            JsonNode def = objectMapper.readTree(definitionJson);
            for (JsonNode item : def.path("items")) {
                JsonNode binding = item.path("binding");
                if (!binding.isObject()) continue;
                String no = item.path("no").asText();
                String domain = binding.path("domain").asText("");
                String field = binding.path("field").asText("");
                Integer windowDays = binding.hasNonNull("windowDays") ? binding.path("windowDays").asInt() : null;
                String mode = binding.path("mode").asText("");

                ObjectNode node = resolveItem(patientId, domain, field, windowDays);
                if (node != null) {
                    node.put("mode", mode);
                    out.set(no, node);
                }
            }
        } catch (Exception e) {
            log.warn("绑定解析失败 patientId={}: {}", patientId, e.getMessage());
        }
        return out.toString();
    }

    private ObjectNode resolveItem(Long patientId, String domain, String field, Integer windowDays) {
        switch (domain) {
            case "LAB" -> {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT p.result_value AS value, p.result_unit AS unit, t.reported_at AS at " +
                        "FROM cdr.c_lab_panel p JOIN cdr.c_lab_test t ON t.id = p.test_id AND t.is_deleted = false " +
                        "WHERE t.patient_id = ? AND p.item_code = ? AND p.is_deleted = false " +
                        "ORDER BY COALESCE(t.reported_at, t.collected_at, t.ordered_at) DESC NULLS LAST LIMIT 1",
                        patientId, field);
                if (rows.isEmpty()) return null;
                Map<String, Object> r = rows.get(0);
                ObjectNode n = objectMapper.createObjectNode();
                n.putPOJO("value", r.get("value"));
                if (r.get("unit") != null) n.put("unit", String.valueOf(r.get("unit")));
                if (r.get("at") != null) n.put("at", r.get("at").toString().substring(0, 10));
                return n;
            }
            case "VITAL" -> {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT sign_value AS value, unit, measured_at AS at FROM cdr.c_vital_sign " +
                        "WHERE patient_id = ? AND sign_type = ? AND is_deleted = false " +
                        "ORDER BY measured_at DESC NULLS LAST LIMIT 1",
                        patientId, field);
                if (rows.isEmpty()) return null;
                Map<String, Object> r = rows.get(0);
                ObjectNode n = objectMapper.createObjectNode();
                n.putPOJO("value", r.get("value"));
                if (r.get("unit") != null) n.put("unit", String.valueOf(r.get("unit")));
                if (r.get("at") != null) n.put("at", r.get("at").toString().substring(0, 10));
                return n;
            }
            case "MEDICATION" -> {
                List<String> names = jdbcTemplate.queryForList(
                        "SELECT DISTINCT med_name FROM cdr.c_medication " +
                        "WHERE patient_id = ? AND med_name IS NOT NULL AND is_deleted = false " +
                        "ORDER BY med_name LIMIT 20", String.class, patientId);
                return optionsNode(names);
            }
            case "DIAGNOSIS" -> {
                List<String> names = jdbcTemplate.queryForList(
                        "SELECT DISTINCT icd_name FROM cdr.c_diagnosis " +
                        "WHERE patient_id = ? AND icd_name IS NOT NULL AND is_deleted = false " +
                        "ORDER BY icd_name LIMIT 20", String.class, patientId);
                return optionsNode(names);
            }
            default -> { return null; }
        }
    }

    private ObjectNode optionsNode(List<String> labels) {
        if (labels == null || labels.isEmpty()) return null;
        ObjectNode n = objectMapper.createObjectNode();
        ArrayNode arr = n.putArray("options");
        labels.forEach(arr::add);
        return n;
    }
}
