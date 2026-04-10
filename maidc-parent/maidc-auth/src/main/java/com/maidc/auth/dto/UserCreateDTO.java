package com.maidc.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度8-64")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 50, message = "姓名长度2-50")
    private String realName;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    @NotEmpty(message = "角色不能为空")
    private List<Long> roleIds;

    @NotNull(message = "组织不能为空")
    private Long orgId;

    private Boolean mustChangePwd = false;
}
