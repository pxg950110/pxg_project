package com.maidc.common.security.scope;

import com.maidc.common.security.context.PermissionContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataScopeHelperTest {

    private PermissionContext ctx(DataScope scope, Long deptId) {
        return PermissionContext.builder().userId(9L).dataScope(scope).deptId(deptId).build();
    }

    @Test
    void allScope_noFilter() {
        assertFalse(DataScopeHelper.needDeptFilter(ctx(DataScope.ALL, 1L)));
    }

    @Test
    void selfAndProjectScopes_noDeptFilter() {
        assertFalse(DataScopeHelper.needDeptFilter(ctx(DataScope.SELF, null)));
        assertFalse(DataScopeHelper.needDeptFilter(ctx(DataScope.PROJECT, null)));
    }

    @Test
    void deptScope_filterByDept() {
        assertTrue(DataScopeHelper.needDeptFilter(ctx(DataScope.DEPT, 3L)));
        assertEquals(3L, DataScopeHelper.deptId(ctx(DataScope.DEPT, 3L)));
    }

    @Test
    void selfScope_createdBy() {
        assertEquals(9L, DataScopeHelper.selfUserId(ctx(DataScope.SELF, null)));
    }

    @Test
    void projectScope_idsFromContext() {
        PermissionContext c = PermissionContext.builder()
                .userId(9L).dataScope(DataScope.PROJECT)
                .projectIds(List.of(1L, 2L)).build();
        assertEquals(List.of(1L, 2L), DataScopeHelper.projectIds(c));
    }

    @Test
    void nullCtx_safeDefaults() {
        assertFalse(DataScopeHelper.needDeptFilter(null));
        assertNull(DataScopeHelper.deptId(null));
        assertNull(DataScopeHelper.selfUserId(null));
        assertTrue(DataScopeHelper.projectIds(null).isEmpty());
    }
}
