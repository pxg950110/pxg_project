package com.maidc.data.service.followup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 量表引擎单测：校验 / 服务端计分 / 归一化 / 绑定项豁免 */
class ScaleEngineTest {

    private ScaleEngine engine;

    private static final String LIKERT = """
        {"type":"LIKERT_ITEMS","maxScore":10,"mcid":null,
         "itemOptions":[{"value":0,"label":"无"},{"value":1,"label":"轻"},{"value":2,"label":"重"}],
         "items":[{"no":"1","text":"条目一"},{"no":"2","text":"条目二"},{"no":"3","text":"条目三","required":false}]}
        """;

    private static final String MIXED = """
        {"type":"MIXED","maxScore":5,"mcid":null,"interpretation":[],
         "items":[
           {"no":"1","type":"NUMBER","text":"IgE","max":5000,
            "binding":{"domain":"LAB","field":"TIGE","mode":"AUTO_READONLY","windowDays":180}},
           {"no":"2","type":"NUMBER","text":"体重","max":300,
            "binding":{"domain":"VITAL","field":"WEIGHT","mode":"AUTO_EDITABLE"}},
           {"no":"3","type":"CHECKBOX","text":"用药","options":[],
            "binding":{"domain":"MEDICATION","field":"ACTIVE_MEDS","mode":"OPTIONS"}},
           {"no":"4","type":"INPUT","text":"备注","required":false},
           {"no":"5","type":"SCORE_RADIO","text":"疼痛","options":[{"value":0,"label":"无"},{"value":5,"label":"重"}]}
         ]}
        """;

    @BeforeEach
    void setUp() {
        engine = new ScaleEngine(new ObjectMapper());
    }

    @Test
    void score_likert_items_use_item_options_fallback() {
        Map<String, Object> answers = Map.of("1", 2, "2", 1);
        assertEquals(3, engine.score(engine.parse(LIKERT), answers));
    }

    @Test()
    void validate_missing_required_item_throws_400() {
        Map<String, Object> answers = new HashMap<>(Map.of("1", 0)); // 缺 no=2
        BusinessException ex = assertThrows(BusinessException.class,
                () -> engine.validateAnswers(engine.parse(LIKERT), answers));
        assertTrue(ex.getMessage().contains("未作答"));
    }

    @Test
    void validate_value_out_of_options_throws() {
        Map<String, Object> answers = Map.of("1", 0, "2", 9);
        assertThrows(BusinessException.class, () -> engine.validateAnswers(engine.parse(LIKERT), answers));
    }

    @Test
    void validate_optional_item_absent_ok() {
        Map<String, Object> answers = Map.of("1", 0, "2", 0); // no=3 非必答
        assertDoesNotThrow(() -> engine.validateAnswers(engine.parse(LIKERT), answers));
    }

    @Test
    void readonly_binding_item_exempt_from_required_check_and_not_scored_by_system() {
        // 只读绑定项未作答不拦截；NUMBER 可编辑预填计分
        Map<String, Object> answers = new HashMap<>(Map.of("2", 70, "5", 5));
        assertDoesNotThrow(() -> engine.validateAnswers(engine.parse(MIXED), answers));
        assertEquals(75, engine.score(engine.parse(MIXED), answers)); // 70 + 5
    }

    @Test
    void number_out_of_max_throws() {
        Map<String, Object> answers = Map.of("2", 9999, "5", 0);
        assertThrows(BusinessException.class, () -> engine.validateAnswers(engine.parse(MIXED), answers));
    }

    @Test
    void checkbox_array_answer_validated_and_not_scored() {
        Map<String, Object> answers = new HashMap<>(Map.of("2", 60, "5", 0, "3", List.of("布地奈德", "孟鲁司特")));
        assertDoesNotThrow(() -> engine.validateAnswers(engine.parse(MIXED), answers));
        assertEquals(60, engine.score(engine.parse(MIXED), answers));
    }

    @Test
    void normalize_answers_keeps_known_items_only() {
        Map<String, Object> answers = new HashMap<>(Map.of("2", 70, "4", "自述好转", "9", 1));
        String json = engine.normalizeAnswers(engine.parse(MIXED), answers);
        assertTrue(json.contains("\"2\":70.0"));
        assertTrue(json.contains("\"4\":\"自述好转\""));
        assertFalse(json.contains("\"9\""), "未知条目应被剔除");
        assertFalse(json.contains("\"1\""), "未作答条目不应出现");
    }

    @Test
    void illegal_definition_throws() {
        assertThrows(BusinessException.class, () -> engine.parse("{\"maxScore\":10}"));
        assertThrows(BusinessException.class, () -> engine.parse("not-json"));
    }
}
