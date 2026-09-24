package com.maidc.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenDTO {

    /** 前端契约发 snake_case refresh_token，camelCase 别名保留服务间调用兼容 */
    @JsonAlias("refresh_token")
    @NotBlank(message = "refresh_token不能为空")
    private String refreshToken;
}
