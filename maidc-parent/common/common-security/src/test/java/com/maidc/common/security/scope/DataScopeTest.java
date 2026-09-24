package com.maidc.common.security.scope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataScopeTest {

    @Test
    void widest_allBeatsEverything() {
        assertEquals(DataScope.ALL, DataScope.widest(DataScope.ALL, DataScope.DEPT));
        assertEquals(DataScope.ALL, DataScope.widest(DataScope.PROJECT, DataScope.ALL));
        assertEquals(DataScope.ALL, DataScope.widest(DataScope.ALL, DataScope.SELF));
    }

    @Test
    void widest_deptBeatsProjectAndSelf() {
        assertEquals(DataScope.DEPT, DataScope.widest(DataScope.DEPT, DataScope.PROJECT));
        assertEquals(DataScope.DEPT, DataScope.widest(DataScope.SELF, DataScope.DEPT));
        assertEquals(DataScope.DEPT, DataScope.widest(DataScope.DEPT, DataScope.DEPT));
    }

    @Test
    void widest_projectBeatsSelf() {
        assertEquals(DataScope.PROJECT, DataScope.widest(DataScope.PROJECT, DataScope.SELF));
        assertEquals(DataScope.PROJECT, DataScope.widest(DataScope.SELF, DataScope.PROJECT));
    }

    @Test
    void widest_selfPlusSelfIsSelf() {
        assertEquals(DataScope.SELF, DataScope.widest(DataScope.SELF, DataScope.SELF));
    }

    @Test
    void widest_nullArgsFailFast() {
        assertThrows(NullPointerException.class, () -> DataScope.widest(null, DataScope.SELF));
        assertThrows(NullPointerException.class, () -> DataScope.widest(DataScope.ALL, null));
    }
}
