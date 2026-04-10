package com.maidc.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientCreateDTO {

    @NotBlank(message = "患者姓名不能为空")
    @Size(max = 64, message = "患者姓名最长64个字符")
    private String name;

    private String gender;

    private LocalDate birthDate;

    @Size(max = 128, message = "身份证哈希最长128个字符")
    private String idCardHash;

    @Size(max = 128, message = "手机号哈希最长128个字符")
    private String phoneHash;

    @Size(max = 256, message = "地址最长256个字符")
    private String address;

    private Long orgId;
}
