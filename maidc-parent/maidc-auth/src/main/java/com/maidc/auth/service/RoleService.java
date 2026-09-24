package com.maidc.auth.service;

import com.maidc.auth.entity.PermissionEntity;
import com.maidc.auth.entity.RoleEntity;
import com.maidc.auth.entity.RolePermissionEntity;
import com.maidc.auth.repository.PermissionRepository;
import com.maidc.auth.repository.RolePermissionRepository;
import com.maidc.auth.repository.RoleRepository;
import com.maidc.auth.repository.UserRoleRepository;
import com.maidc.auth.vo.PermissionTreeVO;
import com.maidc.auth.vo.RoleVO;
import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionCacheService permissionCacheService;

    public List<RoleVO> listRoles() {
        List<RoleEntity> roles = roleRepository.findByIsDeletedFalse();
        return roles.stream().map(role -> {
            List<RolePermissionEntity> rpList = rolePermissionRepository.findByRoleId(role.getId());
            List<Long> permIds = rpList.stream().map(RolePermissionEntity::getPermissionId).toList();
            List<String> permCodes = permIds.isEmpty() ? List.of()
                    : permissionRepository.findByIdInAndIsDeletedFalse(permIds).stream()
                    .map(PermissionEntity::getPermissionCode).toList();
            int userCount = userRoleRepository.findByRoleId(role.getId()).size();

            return RoleVO.builder()
                    .id(role.getId())
                    .code(role.getRoleCode())
                    .name(role.getRoleName())
                    .description(role.getDescription())
                    .permissions(permCodes)
                    .userCount(userCount)
                    .isSystem(role.getIsSystem())
                    .createdAt(role.getCreatedAt())
                    .build();
        }).toList();
    }

    @Transactional
    public RoleVO createRole(String name, String code, String description, List<Long> permissionIds, Long orgId) {
        if (roleRepository.existsByRoleCodeAndOrgIdAndIsDeletedFalse(code, orgId)) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        RoleEntity role = new RoleEntity();
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setDescription(description);
        role.setOrgId(orgId);
        role = roleRepository.save(role);

        assignPermissions(role.getId(), permissionIds);

        log.info("角色创建成功: code={}", code);
        return RoleVO.builder()
                .id(role.getId())
                .code(role.getRoleCode())
                .name(role.getRoleName())
                .description(role.getDescription())
                .build();
    }

    @Transactional
    public RoleVO updateRole(Long id, String description, List<Long> permissionIds) {
        RoleEntity role = roleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (role.getIsSystem()) {
            throw new BusinessException(ErrorCode.SYSTEM_ROLE_IMMUTABLE);
        }

        if (description != null) role.setDescription(description);
        role = roleRepository.save(role);

        if (permissionIds != null) {
            assignPermissions(id, permissionIds);
        }

        return RoleVO.builder()
                .id(role.getId())
                .code(role.getRoleCode())
                .name(role.getRoleName())
                .description(role.getDescription())
                .build();
    }

    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionRepository.deleteByRoleId(roleId);
        if (permissionIds != null) {
            for (Long permId : permissionIds) {
                RolePermissionEntity rp = new RolePermissionEntity();
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                rolePermissionRepository.save(rp);
            }
        }
        // 角色权限增删后失效持有者缓存（提交后执行；createRole 调用此方法时新角色无用户，失效为空操作）
        permissionCacheService.evictByRoleAfterCommit(roleId);
    }

    public List<PermissionTreeVO> getPermissionTree() {
        List<PermissionEntity> allPerms = permissionRepository.findByIsDeletedFalseOrderBySortOrder();
        return buildTree(allPerms, null);
    }

    private List<PermissionTreeVO> buildTree(List<PermissionEntity> allPerms, Long parentId) {
        return allPerms.stream()
                .filter(p -> Objects.equals(p.getParentId(), parentId))
                .map(p -> PermissionTreeVO.builder()
                        .id(p.getId())
                        .code(p.getPermissionCode())
                        .name(p.getPermissionName())
                        .resourceType(p.getResourceType())
                        .description(p.getResourceKey())
                        .children(buildTree(allPerms, p.getId()))
                        .build())
                .toList();
    }

    // ==================== 权限 CRUD（前端 system.ts 契约：{name, code, type, parent_id}） ====================

    /**
     * 创建权限。resource_key/action 为 DDL NOT NULL 列且前端不传，
     * 按 code 约定（resource:action）派生，缺省 action 记为 access。
     */
    @Transactional
    public PermissionTreeVO createPermission(String name, String code, String type, Long parentId) {
        if (name == null || code == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        PermissionEntity p = new PermissionEntity();
        p.setPermissionName(name);
        p.setPermissionCode(code);
        p.setResourceType(type != null ? type : "API");
        applyDerivedParts(p, code);
        p.setParentId(parentId);
        // s_permission.org_id NOT NULL：权限为系统级资源，落 0（与 DDL 种子、createRole 同约定）
        p.setOrgId(0L);
        p = permissionRepository.save(p);
        return toTreeVO(p);
    }

    /** 更新权限（部分字段更新，null 不动）；@Where 使已删除记录查不到即 NOT_FOUND */
    @Transactional
    public PermissionTreeVO updatePermission(Long id, String name, String code, String type, Long parentId) {
        PermissionEntity p = permissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (name != null) {
            p.setPermissionName(name);
        }
        if (code != null) {
            p.setPermissionCode(code);
            applyDerivedParts(p, code);
        }
        if (type != null) {
            p.setResourceType(type);
        }
        if (parentId != null) {
            p.setParentId(parentId);
        }
        p = permissionRepository.save(p);
        return toTreeVO(p);
    }

    /**
     * 删除权限（@SQLDelete 软删）。
     * 无外键约定下同步清理 role-permission 关联，避免悬挂引用。
     * 注：权限码变更不影响在线缓存，需等待 TTL 或用户重登刷新。
     */
    @Transactional
    public void deletePermission(Long id) {
        PermissionEntity p = permissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        rolePermissionRepository.deleteByPermissionId(id);
        permissionRepository.delete(p);
    }

    private void applyDerivedParts(PermissionEntity p, String code) {
        int idx = code.indexOf(':');
        if (idx > 0) {
            p.setResourceKey(code.substring(0, idx));
            p.setAction(code.substring(idx + 1));
        } else {
            p.setResourceKey(code);
            p.setAction("access");
        }
    }

    private PermissionTreeVO toTreeVO(PermissionEntity p) {
        return PermissionTreeVO.builder()
                .id(p.getId())
                .code(p.getPermissionCode())
                .name(p.getPermissionName())
                .resourceType(p.getResourceType())
                .description(p.getResourceKey())
                .children(List.of())
                .build();
    }
}
