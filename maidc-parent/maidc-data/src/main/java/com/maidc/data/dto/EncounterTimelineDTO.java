package com.maidc.data.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 就诊时间轴DTO
 */
@Data
public class EncounterTimelineDTO {
    private Long encounterId;
    private String encounterNo;
    private String encounterType; // OUTPATIENT/INPATIENT/EMERGENCY
    private String deptCode;
    private String deptName;
    private LocalDateTime admitTime;
    private LocalDateTime dischargeTime;
    private String mainDiagnosis; // 主诊断
    private String status; // ACTIVE/DISCHARGED
    private Boolean isCurrent; // 是否当前就诊
}
