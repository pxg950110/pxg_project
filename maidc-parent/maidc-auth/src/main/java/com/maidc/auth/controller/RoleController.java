package com.maidc.auth.controller;

import com.maidc.auth.service.RoleService;
import com.maidc.auth.vo.PermissionTreeVO;
import com.maidc.auth.vo.RoleVO;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/roles")
    @PreAuthorize("hasPermission('system:role')")
    public R<List<RoleVO>> listRoles() {
        return R.ok(roleService.listRoles());
    }

    @OperLog(module = "role", operation = "create")
    @PostMapping("/roles")
    @PreAuthorize("hasPermission('system:role')")
    public R<RoleVO> createRole(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String code = (String) body.get("code");
        String description = (String) body.get("description");
        @SuppressWarnings("unchecked")
        List<Long> permissionIds = (List<Long>) body.get("permission_ids");
        Long orgId = body.get("org_id") != null ? ((Number) body.get("org_id")).longValue() : 0L;
        return R.ok(roleService.createRole(name, code, description, permissionIds, orgId));
    }

    @OperLog(module = "role", operation = "update")
    @PutMapping("/roles/{id}")
    @PreAuthorize("hasPermission('system:role')")
    public R<RoleVO> updateRole(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String description = (String) body.get("description");
        @SuppressWarnings("unchecked")
        List<Long> permissionIds = (List<Long>) body.get("permission_ids");
        return R.ok(roleService.updateRole(id, description, permissionIds));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasPermission('system:role')")
    public R<Void> assignPermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        roleService.assignPermissions(id, body.get("permission_ids"));
        return R.ok();
    }

    @GetMapping("/permissions/tree")
    @PreAuthorize("hasPermission('system:role')")
    public R<List<PermissionTreeVO>> getPermissionTree() {
        return R.ok(roleService.getPermissionTree());
    }

    // ==================== 权限 CRUD（前端 system.ts 契约） ====================

    @OperLog(module = "permission", operation = "create")
    @PostMapping("/permissions")
    @PreAuthorize("hasPermission('system:role')")
    public R<PermissionTreeVO> createPermission(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String code = (String) body.get("code");
        String type = (String) body.get("type");
        Long parentId = body.get("parent_id") != null ? ((Number) body.get("parent_id")).longValue() : null;
        return R.ok(roleService.createPermission(name, code, type, parentId));
    }

    @OperLog(module = "permission", operation = "update")
    @PutMapping("/permissions/{id}")
    @PreAuthorize("hasPermission('system:role')")
    public R<PermissionTreeVO> updatePermission(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String code = (String) body.get("code");
        String type = (String) body.get("type");
        Long parentId = body.get("parent_id") != null ? ((Number) body.get("parent_id")).longValue() : null;
        return R.ok(roleService.updatePermission(id, name, code, type, parentId));
    }

    @OperLog(module = "permission", operation = "delete")
    @DeleteMapping("/permissions/{id}")
    @PreAuthorize("hasPermission('system:role')")
    public R<Void> deletePermission(@PathVariable Long id) {
        roleService.deletePermission(id);
        return R.ok();
    }
}
