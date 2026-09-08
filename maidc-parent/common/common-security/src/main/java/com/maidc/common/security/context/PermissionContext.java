package com.maidc.common.security.context;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.maidc.common.security.scope.DataScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/** 当前用户权限集（Redis maidc:auth:perm:{userId} 的值，Jackson 反序列化目标） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionContext {
    private Long userId;
    private Set<String> permissions;
    private DataScope dataScope;
    private Long deptId;
    private List<String> roles;
    private List<Long> projectIds;

    public boolean has(String code) {
        return permissions != null && permissions.contains(code);
    }
}
