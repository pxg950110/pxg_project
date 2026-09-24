package com.maidc.data.service.etl;

import com.maidc.data.entity.EtlFieldMappingEntity;
import com.maidc.data.entity.EtlStepEntity;
import com.maidc.data.repository.EtlFieldMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * transform_type SELECT 生成层契约（Embulk column_options 保持 DIRECT 直通计算列）：
 * - DIRECT/CONSTANT 维持既有行为
 * - MAP      → CASE {expr} END AS target
 * - DATE_FMT → TO_CHAR(source, expr) AS target；expr 为空 → CAST(source AS VARCHAR) AS target
 * - LOOKUP   → (expr) AS target（expr 为受控标量子查询）
 * - EXPRESSION → expr AS target（受控 SQL 片段）
 * - expr 含分号或行注释 → IllegalArgumentException（拼接进查询会破坏/注入语句）
 */
@ExtendWith(MockitoExtension.class)
class EtlConfigGeneratorTest {

    @Mock
    private EtlFieldMappingRepository fieldMappingRepository;

    @InjectMocks
    private EtlConfigGenerator generator;

    private EtlFieldMappingEntity mapping(String source, String target, String type, String expr, String defaultValue) {
        EtlFieldMappingEntity m = new EtlFieldMappingEntity();
        m.setSourceColumn(source);
        m.setTargetColumn(target);
        m.setTransformType(type);
        m.setTransformExpr(expr);
        m.setDefaultValue(defaultValue);
        return m;
    }

    // ------------------------------------------------------ buildSelectClause

    @Test
    void directAndConstantKeepLegacyBehavior() {
        String clause = generator.buildSelectClause(List.of(
                mapping("a", "ta", "DIRECT", null, null),
                mapping(null, "tb", "CONSTANT", null, "x")));
        assertEquals("a", clause);
    }

    @Test
    void mapWrapsExprInCaseEndAliasedToTarget() {
        String clause = generator.buildSelectClause(List.of(
                mapping("gender_code", "gender_name", "MAP",
                        "WHEN gender_code = '1' THEN '男' ELSE '未知'", null)));
        assertEquals("CASE WHEN gender_code = '1' THEN '男' ELSE '未知' END AS gender_name", clause);
    }

    @Test
    void dateFmtUsesToCharAndCastFallback() {
        String withExpr = generator.buildSelectClause(List.of(
                mapping("admit_time", "admit_date", "DATE_FMT", "'YYYY-MM-DD'", null)));
        assertEquals("TO_CHAR(admit_time, 'YYYY-MM-DD') AS admit_date", withExpr);

        String blankExpr = generator.buildSelectClause(List.of(
                mapping("admit_time", "admit_date", "DATE_FMT", "  ", null)));
        assertEquals("CAST(admit_time AS VARCHAR) AS admit_date", blankExpr);
    }

    @Test
    void lookupWrapsExprAsScalarSubquery() {
        String clause = generator.buildSelectClause(List.of(
                mapping("dept_code", "dept_name", "LOOKUP",
                        "SELECT dept_name FROM dim_dept d WHERE d.dept_code = src.dept_code", null)));
        assertEquals("(SELECT dept_name FROM dim_dept d WHERE d.dept_code = src.dept_code) AS dept_name", clause);
    }

    @Test
    void expressionEmitsExprAliasedToTarget() {
        String clause = generator.buildSelectClause(List.of(
                mapping("first_name", "full_name", "EXPRESSION", "upper(first_name)", null)));
        assertEquals("upper(first_name) AS full_name", clause);
    }

    @Test
    void exprWithSemicolonOrLineCommentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> generator.buildSelectClause(List.of(
                mapping("a", "ta", "EXPRESSION", "upper(a); DROP TABLE x", null))));
        assertThrows(IllegalArgumentException.class, () -> generator.buildSelectClause(List.of(
                mapping("a", "ta", "MAP", "WHEN a = '1' -- comment\nTHEN 'x'", null))));
    }

    @Test
    void unknownTransformTypeFallsBackToPassThrough() {
        String clause = generator.buildSelectClause(List.of(
                mapping("a", "ta", "SOMETHING_ELSE", "ignored", null)));
        assertEquals("a", clause);
    }

    // ------------------------------------------------------ full YAML

    private String configFor(List<EtlFieldMappingEntity> mappings) {
        EtlStepEntity step = new EtlStepEntity();
        step.setId(1L);
        step.setSourceSchema("src");
        step.setSourceTable("t_in");
        step.setTargetSchema("cdr");
        step.setTargetTable("t_out");
        when(fieldMappingRepository.findByStepIdAndIsDeletedFalseOrderBySortOrder(1L)).thenReturn(mappings);
        return generator.generateEmbulkConfig(step,
                "h1", 5432, "sdb", "su", "sp",
                "h2", 5433, "tdb", "tu", "tp");
    }

    @Test
    void columnOptionsForComputedTypesPassThroughTargetColumn() {
        String yaml = configFor(List.of(
                mapping("gender_code", "gender_name", "MAP",
                        "WHEN gender_code = '1' THEN '男'", null),
                mapping("admit_time", "admit_date", "DATE_FMT", "'YYYY-MM-DD'", null),
                mapping(null, "const_col", "CONSTANT", null, "v1")));

        assertTrue(yaml.contains("CASE WHEN gender_code = '1' THEN '男' END AS gender_name"));
        assertTrue(yaml.contains("TO_CHAR(admit_time, 'YYYY-MM-DD') AS admit_date"));
        assertTrue(yaml.contains("gender_name: {value_from: gender_name}"));
        assertTrue(yaml.contains("admit_date: {value_from: admit_date}"));
        assertTrue(yaml.contains("const_col: {value: \"v1\"}"));
    }
}
