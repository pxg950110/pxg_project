package com.maidc.common.security.aspect;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.store.PermissionStore;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionAspectTest {

    private final PermissionStore store = mock(PermissionStore.class);
    private final PermissionAspect aspect = new PermissionAspect(store);

    private PermissionContext ctx(String... perms) {
        return PermissionContext.builder().userId(1L)
                .permissions(Set.of(perms)).build();
    }

    @Test
    void passes_whenPermissionPresent() {
        when(store.load(1L)).thenReturn(ctx("cdr:patient:read"));
        aspect.check("cdr:patient:read", 1L);   // 不抛即通过
    }

    @Test
    void throws403_whenPermissionMissing() {
        when(store.load(1L)).thenReturn(ctx("model:deploy"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.check("cdr:patient:read", 1L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void throws401_whenNoUserContext() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.check("cdr:patient:read", null));
        assertEquals(401, ex.getCode());
    }
}
