package com.maidc.common.security.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.security.scope.DataScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionContextTest {

    @Test
    void has_shouldBeNullSafe() {
        PermissionContext noPerms = PermissionContext.builder().userId(1L).build();
        assertNull(noPerms.getPermissions());
        assertFalse(noPerms.has("cdr:patient:read"));

        PermissionContext ctx = PermissionContext.builder()
                .permissions(Set.of("cdr:patient:read"))
                .build();
        assertTrue(ctx.has("cdr:patient:read"));
        assertFalse(ctx.has("cdr:patient:write"));
    }

    @Test
    void jacksonRoundTrip_shouldPreserveAllFields() throws Exception {
        PermissionContext ctx = PermissionContext.builder()
                .userId(42L)
                .permissions(Set.of("cdr:patient:read", "cdr:cohort:write"))
                .dataScope(DataScope.DEPT)
                .deptId(7L)
                .roles(List.of("ADMIN", "DOCTOR"))
                .projectIds(List.of(101L, 202L))
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(ctx);
        PermissionContext copy = mapper.readValue(json, PermissionContext.class);

        assertEquals(ctx.getUserId(), copy.getUserId());
        assertEquals(ctx.getPermissions(), copy.getPermissions());
        assertEquals(ctx.getDataScope(), copy.getDataScope());
        assertEquals(ctx.getDeptId(), copy.getDeptId());
        assertEquals(ctx.getRoles(), copy.getRoles());
        assertEquals(ctx.getProjectIds(), copy.getProjectIds());
    }

    @Test
    void jacksonDeserialization_shouldIgnoreUnknownFields() throws Exception {
        // 模拟字段新增后回滚时 Redis 中的脏数据（含未知字段 futureField）
        String json = "{\"userId\":42,\"permissions\":[\"cdr:patient:read\"],\"futureField\":\"x\"}";
        PermissionContext ctx = new ObjectMapper().readValue(json, PermissionContext.class);
        assertEquals(42L, ctx.getUserId());
        assertTrue(ctx.has("cdr:patient:read"));
    }
}
