package com.maidc.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.auth.dto.RefreshTokenDTO;
import com.maidc.auth.vo.RefreshVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * refresh 契约命名：前端发 {refresh_token}（snake）、读 {access_token}/{expires_in}；
 * 登录保持 camelCase，与刷新响应有意区分。
 */
class RefreshContractNamingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void refreshRequestAcceptsSnakeCaseAlias() throws Exception {
        RefreshTokenDTO dto = objectMapper.readValue(
                "{\"refresh_token\": \"token-from-frontend\"}", RefreshTokenDTO.class);
        assertEquals("token-from-frontend", dto.getRefreshToken());
    }

    @Test
    void refreshResponseEmitsSnakeCaseKeys() throws Exception {
        String json = objectMapper.writeValueAsString(RefreshVO.builder()
                .accessToken("new-access").expiresIn(7200L).build());
        assertTrue(json.contains("\"access_token\""), () -> "缺 access_token: " + json);
        assertTrue(json.contains("\"expires_in\""), () -> "缺 expires_in: " + json);
        assertFalse(json.contains("accessToken"), () -> "不应输出 camelCase: " + json);
    }
}
