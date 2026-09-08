package com.maidc.auth.service;

import com.maidc.auth.entity.RoleEntity;
import com.maidc.auth.entity.RolePermissionEntity;
import com.maidc.auth.repository.PermissionRepository;
import com.maidc.auth.repository.RolePermissionRepository;
import com.maidc.auth.repository.RoleRepository;
import com.maidc.auth.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PermissionCacheService permissionCacheService;

    @InjectMocks
    private RoleService roleService;

    // ==================== assignPermissions tests ====================

    @Test
    void assignPermissions_revokesAndRegrants_thenEvictsRoleHoldersAfterCommit() {
        roleService.assignPermissions(5L, List.of(100L, 101L));

        verify(rolePermissionRepository).deleteByRoleId(5L);
        verify(rolePermissionRepository, times(2)).save(any(RolePermissionEntity.class));
        // 角色权限增删后失效持有者缓存（事务提交后）
        verify(permissionCacheService).evictByRoleAfterCommit(5L);
    }

    @Test
    void assignPermissions_nullList_clearsAllPermissions_thenStillEvicts() {
        roleService.assignPermissions(5L, null);

        verify(rolePermissionRepository).deleteByRoleId(5L);
        verify(rolePermissionRepository, never()).save(any(RolePermissionEntity.class));
        // 清空权限同样是权限变更，必须失效
        verify(permissionCacheService).evictByRoleAfterCommit(5L);
    }

    // ==================== updateRole tests ====================

    @Test
    void updateRole_withPermissionIds_delegatesToAssignAndEvicts() {
        RoleEntity role = new RoleEntity();
        role.setId(5L);
        role.setRoleCode("doctor");
        role.setIsSystem(false);
        when(roleRepository.findByIdAndIsDeletedFalse(5L)).thenReturn(Optional.of(role));
        when(roleRepository.save(role)).thenReturn(role);

        roleService.updateRole(5L, "新描述", List.of(100L));

        verify(rolePermissionRepository).deleteByRoleId(5L);
        verify(permissionCacheService).evictByRoleAfterCommit(5L);
    }

    @Test
    void updateRole_descriptionOnly_doesNotEvict() {
        RoleEntity role = new RoleEntity();
        role.setId(5L);
        role.setRoleCode("doctor");
        role.setIsSystem(false);
        when(roleRepository.findByIdAndIsDeletedFalse(5L)).thenReturn(Optional.of(role));
        when(roleRepository.save(role)).thenReturn(role);

        roleService.updateRole(5L, "新描述", null);

        // 仅改描述不影响权限缓存
        verify(permissionCacheService, never()).evictByRoleAfterCommit(any());
        verify(rolePermissionRepository, never()).deleteByRoleId(5L);
    }

    @Test
    void updateRole_systemRole_throwsAndDoesNotEvict() {
        RoleEntity role = new RoleEntity();
        role.setId(1L);
        role.setRoleCode("admin");
        role.setIsSystem(true);
        when(roleRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(role));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        roleService.updateRole(1L, "desc", List.of(100L)))
                .isInstanceOf(com.maidc.common.core.exception.BusinessException.class);

        verify(permissionCacheService, never()).evictByRoleAfterCommit(any());
        verify(rolePermissionRepository, never()).deleteByRoleId(1L);
    }
}
