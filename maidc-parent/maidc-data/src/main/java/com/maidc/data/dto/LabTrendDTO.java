package com.maidc.data.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 检验趋势DTO - 用于图表可视化
 */
@Data
public class LabTrendDTO {
    /**
     * 检验项目编码
     */
    private String itemCode;

    /**
     * 检验项目名称
     */
    private String itemName;

    /**
     * 单位
     */
    private String unit;

    /**
     * 参考范围下限
     */
    private String referenceRangeLow;

    /**
     * 参考范围上限
     */
    private String referenceRangeHigh;

    /**
     * 趋势数据点列表
     */
    private List<LabTrendPointDTO> dataPoints;

    /**
     * 检验趋势数据点
     */
    @Data
    public static class LabTrendPointDTO {
        /**
         * 数据点ID
         */
        private Long id;

        /**
         * 检验时间
         */
        private LocalDateTime sampleTime;

        /**
         * 结果值
         */
        private String result;

        /**
         * 异常标志 (NORMAL/HIGH/LOW)
         */
        private String abnormalFlag;

        /**
         * 遇诊ID
         */
        private Long encounterId;

        /**
         * 遇诊科室
         */
        private String deptName;

        /**
         * 遇诊类型
         */
        private String encounterType;
    }
}