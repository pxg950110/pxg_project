package com.maidc.data.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 就诊基本信息DTO
 */
@Data
public class EncounterBasicInfoDTO {
    private Long encounterId;
    private String encounterNo;
    private String encounterType;
    private String deptCode;
    private String deptName;
    private String doctorCode;
    private String doctorName;
    private LocalDateTime admitTime;
    private LocalDateTime dischargeTime;
    private String bedNo;
    private String wardCode;
    private String wardName;
    private String mainDiagnosis;
    private String severity;
    private String status;
}
