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

    /** 患者号，缺省时后端自动生成（P + 时间戳） */
    @Size(max = 64, message = "患者号最长64个字符")
    private String patientNo;

    /** DDL NOT NULL（CHECK M/F/O），缺失走 400 校验而非库约束 500 */
    @NotBlank(message = "性别不能为空")
    private String gender;

    private LocalDate birthDate;

    /** DDL NOT NULL，缺失走 400 校验而非库约束 500 */
    @NotBlank(message = "身份证号不能为空")
    @Size(max = 32, message = "身份证号最长32个字符")
    private String idCardNo;

    @Size(max = 8, message = "血型最长8个字符")
    private String bloodType;

    @Size(max = 32, message = "手机号最长32个字符")
    private String phone;

    @Size(max = 256, message = "地址最长256个字符")
    private String address;

    private Long orgId;
}
