package com.maidc.common.security.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.security.context.PermissionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** 权限集读写：Redis 为主，miss 时调 auth 内部端点懒加载重建 */
@Slf4j
@Component
public class PermissionStore {

    public static final String KEY_PREFIX = "maidc:auth:perm:";
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public PermissionStore(StringRedisTemplate redis, ObjectMapper objectMapper,
            @Value("${maidc.auth.base-url:http://localhost:8081}") String authBaseUrl) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        // 显式超时：JDK 默认 connect/read timeout=0（无限），auth 降级时会挂死请求线程
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(2))
                .withReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder()
                .baseUrl(authBaseUrl)
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }

    /**
     * 读取用户权限集：Redis 命中直接返回；miss/损坏时调 auth 内部端点懒加载并写回缓存。
     * <p>自递归防护：maidc-auth 自身也依赖本模块、两个 bean 都在其上下文中，
     * 内部端点 /api/v1/internal/permissions/** 必须标注 @PublicEndpoint，
     * 绝不可标注 @RequirePermission —— 否则 Redis miss 时懒加载会经切面对自身发起 HTTP 调用，形成无限递归。
     */
    public PermissionContext load(Long userId) {
        String json = redis.opsForValue().get(KEY_PREFIX + userId);
        if (json != null) {
            try {
                PermissionContext ctx = objectMapper.readValue(json, PermissionContext.class);
                ctx.setUserId(userId);
                return ctx;
            } catch (Exception e) {
                log.warn("权限缓存反序列化失败 userId={}, 重建", userId, e);
            }
        }
        PermissionContext ctx = fetchFromAuth(userId);
        if (ctx != null) {
            // auth 端返回体可能不含 userId，回填防止写出键 maidc:auth:perm:null
            ctx.setUserId(userId);
            save(ctx);
        }
        return ctx;
    }

    /** 懒加载：调 auth 内部端点（auth 端会写回 Redis 并返回）。4xx 视为无权限（返回 null），5xx 保持抛错告警 */
    PermissionContext fetchFromAuth(Long userId) {
        return restClient.get()
                .uri("/api/v1/internal/permissions/{userId}", userId)
                .retrieve()
                // 空处理器吞掉 4xx（默认处理器会抛 HttpClientErrorException → 500）；
                // 空响应体解析为 null，由调用方 null 检查转成干净的 403
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> { })
                .body(PermissionContext.class);
    }

    public void save(PermissionContext ctx) {
        try {
            redis.opsForValue().set(KEY_PREFIX + ctx.getUserId(),
                    objectMapper.writeValueAsString(ctx), Duration.ofHours(12));
        } catch (Exception e) {
            log.error("权限缓存写入失败 userId={}", ctx.getUserId(), e);
        }
    }

    public void evict(Long userId) {
        if (userId == null) {
            return; // 防止删除字面量键 maidc:auth:perm:null
        }
        redis.delete(KEY_PREFIX + userId);
    }
}
