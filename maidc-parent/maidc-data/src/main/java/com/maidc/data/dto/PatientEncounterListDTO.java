package com.maidc.data.dto;

import lombok.Data;
import java.util.List;

/**
 * 患者就诊列表DTO
 */
@Data
public class PatientEncounterListDTO {
    private Long patientId;
    private String patientName;
    private String gender;
    private Integer age;
    private String patientNo;
    private String idCard; // 脱敏
    private String phone; // 脱敏
    private String allergyHistory;
    private String familyHistory;
    private List<EncounterTimelineDTO> encounters;
}
