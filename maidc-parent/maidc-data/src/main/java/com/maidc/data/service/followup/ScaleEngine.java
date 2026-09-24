package com.maidc.data.service.followup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maidc.common.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;

/**
 * 量表引擎：definition 解析 / answers 逐条校验 / 服务端计分。
 * 不信任前端分数；AUTO_READONLY 绑定项由系统带入、豁免必答校验。
 */
@Component
@RequiredArgsConstructor
public class ScaleEngine {

    private final ObjectMapper objectMapper;

    public JsonNode parse(String definitionJson) {
        try {
            JsonNode node = objectMapper.readTree(definitionJson);
            if (node.path("items").isArray() && node.path("items").size() > 0) {
                return node;
            }
        } catch (Exception ignored) { }
        throw new BusinessException(500, "量表定义非法（缺少 items）");
    }

    /** item.options 缺省时回退量表级 itemOptions（种子数据用该形式压缩存储） */
    private JsonNode optionsOf(JsonNode def, JsonNode item) {
        if (item.path("options").isArray() && item.path("options").size() > 0) return item.get("options");
        return def.path("itemOptions");
    }

    private boolean isScoreType(String type) {
        return type == null || type.isBlank() || "SCORE_RADIO".equals(type) || "NUMBER".equals(type);
    }

    /** 系统管理的绑定项：只读带入（系统负责）与选项源（无业务数据时允许空勾选）豁免必答校验 */
    private boolean isSystemManagedBinding(JsonNode item) {
        JsonNode b = item.path("binding");
        if (!b.isObject()) return false;
        String mode = b.path("mode").asText(null);
        return "AUTO_READONLY".equals(mode) || "OPTIONS".equals(mode);
    }

    /**
     * 逐条校验 answers：键 = item.no。
     * 抛 400：缺必答项 / 值不在选项集 / 数字越界。
     */
    public void validateAnswers(JsonNode def, Map<String, Object> answers) {
        for (Iterator<JsonNode> it = def.get("items").elements(); it.hasNext(); ) {
            JsonNode item = it.next();
            String no = item.path("no").asText();
            String type = item.path("type").asText(null);
            boolean required = item.path("required").asBoolean(true);
            Object raw = answers.get(no);

            // 分组标题/说明/系统管理绑定项不校验必答
            if ("SECTION".equals(type) || "NOTE".equals(type) || isSystemManagedBinding(item)) continue;

            if (raw == null) {
                if (required) throw new BusinessException(400, "量表条目未作答: " + no);
                continue;
            }

            JsonNode options = optionsOf(def, item);
            switch (type == null ? "SCORE_RADIO" : type) {
                case "SCORE_RADIO", "RADIO" -> {
                    int v = asInt(raw, no);
                    if (!hasOption(options, v)) throw new BusinessException(400, "条目 " + no + " 值不在选项集: " + v);
                }
                case "CHECKBOX" -> {
                    if (!(raw instanceof java.util.List<?> list)) throw new BusinessException(400, "条目 " + no + " 应为数组");
                    for (Object o : list) {
                        if (options.isArray() && !hasOptionLabel(options, String.valueOf(o))) {
                            throw new BusinessException(400, "条目 " + no + " 选项非法: " + o);
                        }
                    }
                }
                case "SELECT" -> {
                    if (options.isArray() && !hasOptionLabel(options, String.valueOf(raw))) {
                        throw new BusinessException(400, "条目 " + no + " 选项非法: " + raw);
                    }
                }
                case "NUMBER" -> {
                    double v = asDouble(raw, no);
                    double max = item.path("max").asDouble(10);
                    if (v < 0 || v > max) throw new BusinessException(400, "条目 " + no + " 数值越界: " + v);
                }
                case "INPUT" -> { /* 文本不限制 */ }
                default -> { /* 未知类型按文本放行 */ }
            }
        }
    }

    /** 服务端计分：SCORE_RADIO 取选项 value，NUMBER 取数值；maxScore=0 的采集类量表不计分 */
    public int score(JsonNode def, Map<String, Object> answers) {
        if (def.path("maxScore").asInt(0) == 0) return 0;
        int total = 0;
        for (Iterator<JsonNode> it = def.get("items").elements(); it.hasNext(); ) {
            JsonNode item = it.next();
            String no = item.path("no").asText();
            String type = item.path("type").asText(null);
            Object raw = answers.get(no);
            if (raw == null) continue;
            if (isScoreType(type)) {
                if ("NUMBER".equals(type)) {
                    total += (int) Math.round(asDouble(raw, no));
                } else {
                    JsonNode opt = findOption(optionsOf(def, item), asInt(raw, no));
                    total += opt == null ? 0 : opt.path("value").asInt(0);
                }
            }
        }
        return total;
    }

    /** 归一化 answers 快照：剔除未知条目、规范化数值，供 c_scale_assessment.answers 落库 */
    public String normalizeAnswers(JsonNode def, Map<String, Object> answers) {
        ObjectNode out = objectMapper.createObjectNode();
        for (Iterator<JsonNode> it = def.get("items").elements(); it.hasNext(); ) {
            JsonNode item = it.next();
            String no = item.path("no").asText();
            String type = item.path("type").asText(null);
            Object raw = answers.get(no);
            if (raw == null) continue;
            switch (type == null ? "SCORE_RADIO" : type) {
                case "SCORE_RADIO", "RADIO" -> out.put(no, asInt(raw, no));
                case "NUMBER" -> out.put(no, asDouble(raw, no));
                case "CHECKBOX" -> {
                    ArrayNode arr = out.putArray(no);
                    ((java.util.List<?>) raw).forEach(o -> arr.add(String.valueOf(o)));
                }
                default -> out.put(no, String.valueOf(raw));
            }
        }
        return out.toString();
    }

    private int asInt(Object raw, String no) {
        if (raw instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(raw)); }
        catch (NumberFormatException e) { throw new BusinessException(400, "条目 " + no + " 值非法: " + raw); }
    }

    private double asDouble(Object raw, String no) {
        if (raw instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(raw)); }
        catch (NumberFormatException e) { throw new BusinessException(400, "条目 " + no + " 值非法: " + raw); }
    }

    private boolean hasOption(JsonNode options, int value) {
        return findOption(options, value) != null;
    }

    private JsonNode findOption(JsonNode options, int value) {
        if (!options.isArray()) return null;
        for (Iterator<JsonNode> it = options.elements(); it.hasNext(); ) {
            JsonNode o = it.next();
            if (o.path("value").asInt(Integer.MIN_VALUE) == value) return o;
        }
        return null;
    }

    private boolean hasOptionLabel(JsonNode options, String label) {
        // OPTIONS 动态枚举模式下画布静态选项为空（0 项）→ 放行由枚举来源保证
        if (!options.isArray() || options.isEmpty()) return true;
        for (Iterator<JsonNode> it = options.elements(); it.hasNext(); ) {
            if (it.next().path("label").asText().equals(label)) return true;
        }
        return false;
    }
}
