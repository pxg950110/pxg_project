package com.maidc.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPwdDTO {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度8-64")
    private String newPassword;

    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;
}
