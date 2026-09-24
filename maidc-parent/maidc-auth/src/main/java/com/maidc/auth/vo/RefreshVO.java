package com.maidc.auth.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新令牌响应。
 * <p>前端 request.ts 刷新逻辑按 snake_case 取值（access_token/expires_in），
 * 与 login 的 camelCase LoginVO 有意区分，故独立成 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshVO {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private Long expiresIn;
}
