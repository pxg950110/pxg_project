package com.maidc.auth.controller;

import com.maidc.auth.dto.ResetPwdDTO;
import com.maidc.auth.dto.UserCreateDTO;
import com.maidc.auth.dto.UserUpdateDTO;
import com.maidc.auth.service.UserService;
import com.maidc.auth.vo.UserDetailVO;
import com.maidc.auth.vo.UserVO;
import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.common.security.util.JwtUtils;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    @GetMapping("/me")
    public R<CurrentUserVO> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(token);
        UserDetailVO user = userService.getUser(userId);
        CurrentUserVO vo = CurrentUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roles(user.getRoles().stream().map(r -> r.getCode()).toList())
                .orgId(user.getOrgId())
                .permissions(List.of())
                .build();
        return R.ok(vo);
    }

    @GetMapping
    @PreAuthorize("hasPermission('system:user')")
    public R<PageResult<UserVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return R.ok(userService.listUsers(page, pageSize, keyword, status));
    }

    @OperLog(module = "user", operation = "create")
    @PostMapping
    @PreAuthorize("hasPermission('system:user')")
    public R<UserVO> createUser(@RequestBody @Valid UserCreateDTO dto) {
        return R.ok(userService.createUser(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('system:user')")
    public R<UserDetailVO> getUser(@PathVariable Long id) {
        return R.ok(userService.getUser(id));
    }

    @OperLog(module = "user", operation = "update")
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('system:user')")
    public R<UserVO> updateUser(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        return R.ok(userService.updateUser(id, dto));
    }

    @OperLog(module = "user", operation = "resetPassword")
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasPermission('system:user')")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody @Valid ResetPwdDTO dto) {
        userService.resetPassword(id, dto.getCurrentPassword(), dto.getNewPassword());
        return R.ok();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentUserVO {
        private Long id;
        private String username;
        private String realName;
        private List<String> roles;
        private Long orgId;
        private List<String> permissions;
    }
}
