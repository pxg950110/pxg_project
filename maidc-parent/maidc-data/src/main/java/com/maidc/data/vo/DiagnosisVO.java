package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisVO {

    private Long id;

    private Long encounterId;

    private Long patientId;

    private String diagnosisCode;

    private String diagnosisName;

    private String diagnosisType;
}
