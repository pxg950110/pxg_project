package com.maidc.auth.controller;

import com.maidc.auth.controller.UserController.CurrentUserVO;
import com.maidc.auth.service.PermissionCacheService;
import com.maidc.auth.service.UserService;
import com.maidc.auth.vo.RoleVO;
import com.maidc.auth.vo.UserDetailVO;
import com.maidc.common.core.result.R;
import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.scope.DataScope;
import com.maidc.common.security.util.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PermissionCacheService permissionCacheService;

    @InjectMocks
    private UserController userController;

    // ==================== getCurrentUser (/me) tests ====================

    @Test
    void getCurrentUser_returnsRealPermissionsAndDataScope() {
        // F5 刷新场景：/me 是前端重建 userInfo 的第二数据源，权限集必须与登录响应同源
        when(jwtUtils.getUserIdFromToken("token")).thenReturn(1L);
        when(userService.getUser(1L)).thenReturn(UserDetailVO.builder()
                .id(1L).username("admin").realName("管理员")
                .roles(List.of(RoleVO.builder().id(10L).code("admin").name("管理员").build()))
                .orgId(0L)
                .build());
        when(permissionCacheService.build(1L)).thenReturn(PermissionContext.builder()
                .userId(1L)
                .permissions(Set.of("cdr:patient:read", "system:user:manage"))
                .dataScope(DataScope.ALL)
                .deptId(30L)
                .roles(List.of("admin"))
                .build());

        R<CurrentUserVO> result = userController.getCurrentUser("Bearer token");

        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getPermissions())
                .containsExactlyInAnyOrder("cdr:patient:read", "system:user:manage");
        assertThat(result.getData().getDataScope()).isEqualTo("ALL");
        assertThat(result.getData().getRoles()).containsExactly("admin");
        assertThat(result.getData().getUsername()).isEqualTo("admin");
    }

    @Test
    void getCurrentUser_cacheBuildReturnsNull_fallsBackToEmptyAndSelf() {
        when(jwtUtils.getUserIdFromToken("token")).thenReturn(1L);
        when(userService.getUser(1L)).thenReturn(UserDetailVO.builder()
                .id(1L).username("admin").realName("管理员")
                .roles(List.of())
                .orgId(0L)
                .build());
        when(permissionCacheService.build(1L)).thenReturn(null);

        R<CurrentUserVO> result = userController.getCurrentUser("Bearer token");

        assertThat(result.getData().getPermissions()).isEmpty();
        assertThat(result.getData().getDataScope()).isEqualTo("SELF");
    }
}
