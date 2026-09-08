package com.maidc.common.security.aspect;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.security.audit.PermissionAuditPublisher;
import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.store.PermissionStore;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PermissionAspectTest {

    private final PermissionStore store = mock(PermissionStore.class);
    private final PermissionAuditPublisher publisher = mock(PermissionAuditPublisher.class);
    private final PermissionAspect aspect = new PermissionAspect(store, publisher);

    private PermissionContext ctx(String... perms) {
        return PermissionContext.builder().userId(1L)
                .permissions(Set.of(perms)).build();
    }

    @Test
    void passes_whenPermissionPresent() {
        when(store.load(1L)).thenReturn(ctx("cdr:patient:read"));
        aspect.check("cdr:patient:read", 1L);   // 不抛即通过
        verify(publisher, never()).publishDenied(any(), any(), any());
    }

    @Test
    void throws403_whenPermissionMissing() {
        when(store.load(1L)).thenReturn(ctx("model:deploy"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.check("cdr:patient:read", 1L));
        assertEquals(403, ex.getCode());
        verify(publisher).publishDenied(eq(1L), eq("cdr:patient:read"), any());
    }

    @Test
    void throws401_whenNoUserContext() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.check("cdr:patient:read", null));
        assertEquals(401, ex.getCode());
        verify(publisher, never()).publishDenied(any(), any(), any());
    }
}
