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

    private String name;

    private String gender;

    private LocalDate birthDate;

    private String address;

    private Long orgId;

    private String idCardHash;

    private String phoneHash;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
