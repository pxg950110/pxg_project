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
            throw new BusinessException(403, "系统内置角色不可修改");
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
}
