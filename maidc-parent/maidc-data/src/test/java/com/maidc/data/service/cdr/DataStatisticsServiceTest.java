package com.maidc.data.service.cdr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 月度增长趋势的多表 LEFT JOIN 必须按系列去重计数：
 * 月轴同一行 JOIN 四张一对多表会产生笛卡尔扇出，COUNT(col) 会放大为交叉积，
 * 必须 COUNT(DISTINCT pk) —— 这是图表唯一的数据语义，错即全错。
 */
@ExtendWith(MockitoExtension.class)
class DataStatisticsServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DataStatisticsService service;

    @Test
    void growthTrendCountsEachSeriesDistinctToSurviveJoinFanOut() {
        org.mockito.Mockito.doNothing().when(jdbcTemplate)
                .query(anyString(), any(RowCallbackHandler.class), any(Object[].class));

        service.getDataGrowthTrend(6);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowCallbackHandler.class), any(Object[].class));
        String sql = sqlCaptor.getValue();

        assertTrue(sql.contains("COUNT(DISTINCT lt.id)  AS clinical"), () -> "clinical 系列未去重:\n" + sql);
        assertTrue(sql.contains("COUNT(DISTINCT cn.id)  AS research"), () -> "research 系列未去重:\n" + sql);
        assertTrue(sql.contains("COUNT(DISTINCT ie.id)  AS imaging"), () -> "imaging 系列未去重:\n" + sql);
        assertTrue(sql.contains("COUNT(DISTINCT pa.id)  AS pathology"), () -> "pathology 系列未去重:\n" + sql);
    }

    @Test
    void growthTrendClampsMonthsToConfiguredBounds() {
        org.mockito.Mockito.doNothing().when(jdbcTemplate)
                .query(anyString(), any(RowCallbackHandler.class), any(Object[].class));

        service.getDataGrowthTrend(999);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> argCaptor = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowCallbackHandler.class), argCaptor.capture());
        assertEquals(24, ((Number) argCaptor.getValue()).intValue());
    }

    @Test
    void sourceDistributionMergesBlankSourceIntoUnknown() {
        when(jdbcTemplate.query(anyString(), org.mockito.ArgumentMatchers.any(
                org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(java.util.List.of());

        service.getSourceDistribution();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), org.mockito.ArgumentMatchers.any(
                org.springframework.jdbc.core.RowMapper.class));
        assertTrue(sqlCaptor.getValue().contains("COALESCE(NULLIF(source_system, ''), 'UNKNOWN')"));
    }
}
