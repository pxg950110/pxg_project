package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientVO {

    private Long id;

    /** 患者号（cdr.c_patient.patient_no，业务唯一标识） */
    private String patientNo;

    private String name;

    private String gender;

    private LocalDate birthDate;

    private String address;

    private Long orgId;

    private String idCardNo;

    private String phone;

    private String bloodType;

    private String sourceSystem;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
