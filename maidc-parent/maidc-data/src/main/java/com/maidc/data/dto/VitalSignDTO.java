package com.maidc.data.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生命体征DTO - 用于图表可视化
 */
@Data
public class VitalSignDTO {
    /**
     * 遇诊ID
     */
    private Long encounterId;

    /**
     * 记录时间
     */
    private LocalDateTime recordTime;

    /**
     * 体温 (℃)
     */
    private Double temperature;

    /**
     * 收缩压 (mmHg)
     */
    private Integer systolicBP;

    /**
     * 舒张压 (mmHg)
     */
    private Integer diastolicBP;

    /**
     * 脉搏 (次/分)
     */
    private Integer pulse;

    /**
     * 呼吸频率 (次/分)
     */
    private Integer respirationRate;

    /**
     * 血氧饱和度 (%)
     */
    private Integer oxygenSaturation;

    /**
     * 体重 (kg)
     */
    private Double weight;

    /**
     * 身高 (cm)
     */
    private Double height;

    /**
     * BMI
     */
    private Double bmi;

    /**
     * 记录科室
     */
    private String deptName;

    /**
     * 记录护士
     */
    private String nurseName;
}