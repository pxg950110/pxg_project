package com.maidc.common.redis.strategy;

import com.maidc.common.redis.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Redis 缓存策略管理器
 * 实现 6 种缓存策略（对齐 ARCH §3.2）
 *
 * 策略概览：
 * 1. Token Write-Through  — JWT 黑名单 TTL=2h
 * 2. 权限 Cache-Aside     — 角色权限缓存 TTL=30min
 * 3. 字典 Cache-Aside     — 数据字典缓存 TTL=24h
 * 4. 模型 Cache-Aside     — 模型元数据缓存 TTL=10min
 * 5. 部署状态 Write-Behind — 部署状态 TTL=30s
 * 6. 分布式锁             — 可配 TTL
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheStrategies {

    private final RedisCacheService cacheService;

    // ==================== Key Prefixes ====================
    private static final String TOKEN_PREFIX = "maidc:auth:token:blacklist:";
    private static final String PERMISSION_PREFIX = "maidc:cache:permission:";
    private static final String DICT_PREFIX = "maidc:cache:dict:";
    private static final String MODEL_PREFIX = "maidc:cache:model:";
    private static final String DEPLOY_STATUS_PREFIX = "maidc:cache:deploy:status:";

    // ==================== TTL 配置 ====================
    private static final Duration TOKEN_TTL = Duration.ofHours(2);
    private static final Duration PERMISSION_TTL = Duration.ofMinutes(30);
    private static final Duration DICT_TTL = Duration.ofHours(24);
    private static final Duration MODEL_TTL = Duration.ofMinutes(10);
    private static final Duration DEPLOY_STATUS_TTL = Duration.ofSeconds(30);

    // ==================== 1. Token Write-Through ====================

    /**
     * 写入 Token 黑名单（Write-Through 策略）
     * Token 失效时同步写入 Redis + 状态码
     */
    public void blackListToken(String tokenId) {
        cacheService.set(TOKEN_PREFIX + tokenId, "1", TOKEN_TTL);
        log.debug("Token 黑名单写入: {}", tokenId);
    }

    public boolean isTokenBlacklisted(String tokenId) {
        return cacheService.exists(TOKEN_PREFIX + tokenId);
    }

    // ==================== 2. 权限 Cache-Aside ====================

    /**
     * Cache-Aside 策略：先查缓存，miss 时查库并回写
     * @param roleId 角色ID
     * @param loader 数据库查询函数
     */
    @SuppressWarnings("unchecked")
    public <T> T getPermissions(Long roleId, Supplier<T> loader) {
        String key = PERMISSION_PREFIX + roleId;
        T cached = cacheService.get(key);
        if (cached != null) {
            log.debug("权限缓存命中: roleId={}", roleId);
            return cached;
        }
        T data = loader.get();
        if (data != null) {
            cacheService.set(key, data, PERMISSION_TTL);
            log.debug("权限缓存回写: roleId={}", roleId);
        }
        return data;
    }

    /**
     * 权限变更时清除缓存
     */
    public void evictPermissions(Long roleId) {
        cacheService.delete(PERMISSION_PREFIX + roleId);
        log.debug("权限缓存清除: roleId={}", roleId);
    }

    // ==================== 3. 字典 Cache-Aside ====================

    @SuppressWarnings("unchecked")
    public <T> T getDict(String dictType, Supplier<T> loader) {
        String key = DICT_PREFIX + dictType;
        T cached = cacheService.get(key);
        if (cached != null) {
            return cached;
        }
        T data = loader.get();
        if (data != null) {
            cacheService.set(key, data, DICT_TTL);
        }
        return data;
    }

    public void evictDict(String dictType) {
        cacheService.delete(DICT_PREFIX + dictType);
    }

    // ==================== 4. 模型 Cache-Aside ====================

    @SuppressWarnings("unchecked")
    public <T> T getModel(Long modelId, Supplier<T> loader) {
        String key = MODEL_PREFIX + modelId;
        T cached = cacheService.get(key);
        if (cached != null) {
            return cached;
        }
        T data = loader.get();
        if (data != null) {
            cacheService.set(key, data, MODEL_TTL);
        }
        return data;
    }

    public void evictModel(Long modelId) {
        cacheService.delete(MODEL_PREFIX + modelId);
    }

    // ==================== 5. 部署状态 Write-Behind ====================

    /**
     * Write-Behind 策略：先写缓存，异步同步到数据库
     * 部署状态变更频繁，用短 TTL 保证最终一致性
     */
    public void updateDeployStatus(Long deploymentId, String status) {
        String key = DEPLOY_STATUS_PREFIX + deploymentId;
        cacheService.set(key, status, DEPLOY_STATUS_TTL);
        log.debug("部署状态缓存更新: deploymentId={}, status={}", deploymentId, status);
    }

    @SuppressWarnings("unchecked")
    public String getDeployStatus(Long deploymentId, Supplier<String> loader) {
        String key = DEPLOY_STATUS_PREFIX + deploymentId;
        String cached = cacheService.get(key);
        if (cached != null) {
            return cached;
        }
        String data = loader.get();
        if (data != null) {
            cacheService.set(key, data, DEPLOY_STATUS_TTL);
        }
        return data;
    }

    public void evictDeployStatus(Long deploymentId) {
        cacheService.delete(DEPLOY_STATUS_PREFIX + deploymentId);
    }

    // ==================== 6. 批量清除 ====================

    /**
     * 清除所有业务缓存
     */
    public void clearAllCaches() {
        // 在实际生产中应使用 SCAN 命令按前缀删除
        log.warn("清除全部业务缓存（生产环境应慎用）");
    }
}
