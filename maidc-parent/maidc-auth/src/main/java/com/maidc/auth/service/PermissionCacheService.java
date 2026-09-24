package com.maidc.auth.service;

import com.maidc.auth.entity.PermissionEntity;
import com.maidc.auth.entity.RoleEntity;
import com.maidc.auth.entity.RolePermissionEntity;
import com.maidc.auth.entity.UserEntity;
import com.maidc.auth.entity.UserRoleEntity;
import com.maidc.auth.repository.PermissionRepository;
import com.maidc.auth.repository.RolePermissionRepository;
import com.maidc.auth.repository.RoleRepository;
import com.maidc.auth.repository.UserRepository;
import com.maidc.auth.repository.UserRoleRepository;
import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.scope.DataScope;
import com.maidc.common.security.store.PermissionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限缓存构建：聚合 用户角色 → 角色权限 → 权限码 + 数据范围 + 科室 + 项目成员，
 * 写入 Redis（maidc:auth:perm:{userId}）并返回上下文。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PermissionStore store;

    /**
     * 构建并写入 Redis，返回上下文；用户不存在或非 ACTIVE 返回 null。
     * <p>ACTIVE 防御：内部端点网络可达，禁用用户不得构建/缓存权限集
     * （软删用户已被 @Where 排除）。
     */
    public PermissionContext build(Long userId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            return null;
        }

        List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(userId);
        List<Long> roleIds = userRoles.stream().map(UserRoleEntity::getRoleId).toList();
        List<RoleEntity> roles = roleIds.isEmpty() ? List.of() : roleRepository.findAllById(roleIds);

        List<RolePermissionEntity> rps = roleIds.isEmpty() ? List.of()
                : rolePermissionRepository.findByRoleIdIn(roleIds);
        List<Long> permIds = rps.stream().map(RolePermissionEntity::getPermissionId).distinct().toList();
        List<PermissionEntity> perms = permIds.isEmpty() ? List.of() : permissionRepository.findAllById(permIds);

        DataScope scope = roles.stream()
                .map(r -> DataScope.valueOf(r.getDataScope()))
                .reduce(DataScope.SELF, DataScope::widest);

        // 项目成员（rdr.r_study_member, status=ACTIVE）
        List<Long> projectIds = jdbcTemplate.queryForList(
                "SELECT project_id FROM rdr.r_study_member WHERE user_id = ? AND status = 'ACTIVE'",
                Long.class, userId);

        PermissionContext ctx = PermissionContext.builder()
                .userId(userId)
                .permissions(perms.stream().map(PermissionEntity::getPermissionCode).collect(Collectors.toSet()))
                .dataScope(scope)
                .deptId(user.getDeptId())
                .roles(roles.stream().map(RoleEntity::getRoleCode).toList())
                .projectIds(projectIds)
                .build();
        store.save(ctx);
        log.debug("权限缓存已构建: userId={}, perms={}, scope={}", userId, ctx.getPermissions().size(), scope);
        return ctx;
    }

    /** 用户角色变更 → 失效该用户缓存 */
    public void evictUser(Long userId) {
        store.evict(userId);
    }

    /** 角色的权限/属性变更 → 失效所有持有该角色的用户 */
    public void evictByRole(Long roleId) {
        userRoleRepository.findByRoleId(roleId)
                .forEach(ur -> store.evict(ur.getUserId()));
    }

    /**
     * 事务提交后再失效用户缓存，避免 evict→commit 窗口内并发 build() 用旧数据重建缓存（12h TTL）。
     * 无事务同步上下文（测试/直调）时立即失效。
     */
    public void evictUserAfterCommit(Long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    store.evict(userId);
                }
            });
        } else {
            store.evict(userId);
        }
    }

    /**
     * 事务提交后再失效角色持有者缓存。成员列表在事务内先行收集（提交后原会话查询已不可靠）。
     */
    public void evictByRoleAfterCommit(Long roleId) {
        List<Long> userIds = userRoleRepository.findByRoleId(roleId).stream()
                .map(UserRoleEntity::getUserId)
                .toList();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    userIds.forEach(store::evict);
                }
            });
        } else {
            userIds.forEach(store::evict);
        }
    }
}
