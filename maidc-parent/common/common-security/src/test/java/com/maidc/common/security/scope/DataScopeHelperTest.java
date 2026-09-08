package com.maidc.common.security.scope;

import com.maidc.common.security.context.PermissionContext;
import org.junit.jupiter.api.Test;

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
                .projectIds(java.util.List.of(1L, 2L)).build();
        assertEquals(java.util.List.of(1L, 2L), DataScopeHelper.projectIds(c));
    }
}
