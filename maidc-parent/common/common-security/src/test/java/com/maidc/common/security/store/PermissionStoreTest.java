package com.maidc.common.security.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.security.context.PermissionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionStoreTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private PermissionStore store;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        store = spy(new PermissionStore(redis, objectMapper, "http://localhost:8081"));
    }

    @Test
    void load_redisHit_returnsContextWithUserIdForced_noHttpCall() {
        // Redis 中无 userId 字段（旧数据），load 应回填入参 userId
        when(valueOps.get(PermissionStore.KEY_PREFIX + 1))
                .thenReturn("{\"permissions\":[\"cdr:patient:read\"]}");

        PermissionContext ctx = store.load(1L);

        assertNotNull(ctx);
        assertEquals(1L, ctx.getUserId());
        assertTrue(ctx.has("cdr:patient:read"));
        verify(store, never()).fetchFromAuth(any());
    }

    @Test
    void load_corruptJson_fallsBackToLazyLoad_andSavesWithUserIdBackfilled() {
        when(valueOps.get(PermissionStore.KEY_PREFIX + 1)).thenReturn("{corrupt");
        // auth 端返回体不带 userId（M1：防止写出键 ...:null）
        PermissionContext fromAuth = PermissionContext.builder()
                .permissions(Set.of("cdr:patient:read")).build();
        doReturn(fromAuth).when(store).fetchFromAuth(1L);

        PermissionContext ctx = store.load(1L);

        assertNotNull(ctx);
        assertEquals(1L, ctx.getUserId());
        verify(valueOps).set(eq(PermissionStore.KEY_PREFIX + 1),
                argThat((String json) -> json.contains("\"userId\":1")),
                eq(Duration.ofHours(12)));
    }

    @Test
    void load_lazyReturnsNullBody_returnsNull() {
        when(valueOps.get(PermissionStore.KEY_PREFIX + 1)).thenReturn(null);
        doReturn(null).when(store).fetchFromAuth(1L);

        assertNull(store.load(1L));
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void save_redisThrows_swallowedNoException() {
        doThrow(new RuntimeException("redis down"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));
        PermissionContext ctx = PermissionContext.builder().userId(1L)
                .permissions(Set.of("cdr:patient:read")).build();

        assertDoesNotThrow(() -> store.save(ctx));
    }

    @Test
    void evict_nullUserId_noOpDoesNotTouchRedis() {
        assertDoesNotThrow(() -> store.evict(null));
        verify(redis, never()).delete(anyString());
    }
}
