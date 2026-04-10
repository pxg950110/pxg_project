package com.maidc.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenDTO {

    @NotBlank(message = "refresh_token不能为空")
    private String refreshToken;
}
