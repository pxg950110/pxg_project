package com.maidc.auth.service;

import com.maidc.auth.entity.*;
import com.maidc.auth.repository.*;
import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.scope.DataScope;
import com.maidc.common.security.store.PermissionStore;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PermissionCacheServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final RolePermissionRepository rolePermissionRepository = mock(RolePermissionRepository.class);
    private final PermissionRepository permissionRepository = mock(PermissionRepository.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final PermissionStore store = mock(PermissionStore.class);

    private final PermissionCacheService service = new PermissionCacheService(
            userRepository, userRoleRepository, roleRepository,
            rolePermissionRepository, permissionRepository, jdbcTemplate, store);

    @Test
    void build_permissionSetAndWidestScope() {
        // 用户属于 doctor 角色，科室 30
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setDeptId(30L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserRoleEntity ur = new UserRoleEntity();
        ur.setUserId(1L);
        ur.setRoleId(10L);
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(ur));

        RoleEntity doctor = new RoleEntity();
        doctor.setId(10L);
        doctor.setRoleCode("doctor");
        doctor.setDataScope("DEPT");
        when(roleRepository.findAllById(List.of(10L))).thenReturn(List.of(doctor));

        RolePermissionEntity rp = new RolePermissionEntity();
        rp.setRoleId(10L);
        rp.setPermissionId(100L);
        when(rolePermissionRepository.findByRoleIdIn(List.of(10L))).thenReturn(List.of(rp));

        PermissionEntity perm = new PermissionEntity();
        perm.setId(100L);
        perm.setPermissionCode("cdr:patient:read");
        when(permissionRepository.findAllById(List.of(100L))).thenReturn(List.of(perm));

        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(1L))).thenReturn(List.of());

        PermissionContext ctx = service.build(1L);

        assertNotNull(ctx);
        assertEquals(1L, ctx.getUserId());
        assertTrue(ctx.has("cdr:patient:read"));
        assertFalse(ctx.has("cdr:patient:export"));
        assertEquals(DataScope.DEPT, ctx.getDataScope());
        assertEquals(30L, ctx.getDeptId());
        assertEquals(List.of("doctor"), ctx.getRoles());
        assertTrue(ctx.getProjectIds().isEmpty());
        verify(store).save(ctx);
    }

    @Test
    void build_multipleRoles_widestScopeWins() {
        UserEntity user = new UserEntity();
        user.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        UserRoleEntity ur1 = new UserRoleEntity();
        ur1.setRoleId(10L);
        UserRoleEntity ur2 = new UserRoleEntity();
        ur2.setRoleId(20L);
        when(userRoleRepository.findByUserId(2L)).thenReturn(List.of(ur1, ur2));

        RoleEntity doctor = new RoleEntity();
        doctor.setId(10L);
        doctor.setRoleCode("doctor");
        doctor.setDataScope("DEPT");
        RoleEntity auditor = new RoleEntity();
        auditor.setId(20L);
        auditor.setRoleCode("auditor");
        auditor.setDataScope("ALL");
        when(roleRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(doctor, auditor));

        when(rolePermissionRepository.findByRoleIdIn(List.of(10L, 20L))).thenReturn(List.of());
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(2L))).thenReturn(List.of(42L));

        PermissionContext ctx = service.build(2L);

        // DEPT 与 ALL 并存取更宽的 ALL
        assertEquals(DataScope.ALL, ctx.getDataScope());
        assertEquals(List.of(42L), ctx.getProjectIds());
        verify(store).save(ctx);
    }

    @Test
    void build_noRoles_defaultsToSelfScope() {
        UserEntity user = new UserEntity();
        user.setId(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRoleRepository.findByUserId(3L)).thenReturn(List.of());
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(3L))).thenReturn(List.of());

        PermissionContext ctx = service.build(3L);

        assertEquals(DataScope.SELF, ctx.getDataScope());
        assertTrue(ctx.getPermissions().isEmpty());
        verify(store).save(ctx);
    }

    @Test
    void build_unknownUser_returnsNullWithoutSaving() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(service.build(99L));
        verifyNoInteractions(store);
    }

    @Test
    void build_disabledUser_returnsNullWithoutSaving() {
        UserEntity user = new UserEntity();
        user.setId(4L);
        user.setStatus("DISABLED");
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));

        assertNull(service.build(4L));
        verifyNoInteractions(store);
    }

    @Test
    void evictByRole_clearsCacheOfAffectedUsers() {
        UserRoleEntity ur = new UserRoleEntity();
        ur.setUserId(7L);
        when(userRoleRepository.findByRoleId(5L)).thenReturn(List.of(ur));

        service.evictByRole(5L);

        verify(store).evict(7L);
    }

    @Test
    void evictUser_delegatesToStore() {
        service.evictUser(7L);

        verify(store).evict(7L);
    }

    @Test
    void evictUserAfterCommit_noTransactionContext_evictsImmediately() {
        // 普通 Mock 单测无事务同步上下文 → 走立即失效分支
        service.evictUserAfterCommit(7L);

        verify(store).evict(7L);
    }

    @Test
    void evictByRoleAfterCommit_noTransactionContext_evictsImmediately() {
        UserRoleEntity ur = new UserRoleEntity();
        ur.setUserId(7L);
        when(userRoleRepository.findByRoleId(5L)).thenReturn(List.of(ur));

        service.evictByRoleAfterCommit(5L);

        verify(store).evict(7L);
    }
}
