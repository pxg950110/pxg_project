package com.maidc.auth.controller;

import com.maidc.auth.dto.LoginDTO;
import com.maidc.auth.dto.RefreshTokenDTO;
import com.maidc.auth.service.AuthService;
import com.maidc.auth.vo.LoginVO;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @OperLog(module = "auth", operation = "login")
    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @OperLog(module = "auth", operation = "refresh")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@RequestBody @Valid RefreshTokenDTO dto) {
        return R.ok(authService.refreshToken(dto));
    }

    @OperLog(module = "auth", operation = "logout")
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return R.ok();
    }
}
