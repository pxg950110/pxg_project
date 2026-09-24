package com.maidc.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String realName;
        private List<String> roles;
        private Long orgId;
        /** 权限码集合（前端按钮/菜单级控制） */
        private Set<String> permissions;
        /** 数据范围 ALL/DEPT/SELF/PROJECT */
        private String dataScope;
    }
}
