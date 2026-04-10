package com.maidc.common.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private final ConcurrentMap<String, String> lockOwners = new ConcurrentHashMap<>();

    /**
     * Try to acquire a distributed lock.
     *
     * @param key     lock key (e.g. "maidc:lock:model:eval:{evalId}")
     * @param timeout lock duration
     * @return lock value if acquired, null otherwise
     */
    public String tryLock(String key, Duration timeout) {
        String value = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, timeout);
        if (Boolean.TRUE.equals(acquired)) {
            lockOwners.put(key, value);
            log.debug("获取锁成功: key={}", key);
            return value;
        }
        log.debug("获取锁失败: key={}", key);
        return null;
    }

    /**
     * Release a distributed lock (Lua script ensures atomic check-and-delete).
     */
    public boolean unlock(String key, String value) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(script,
                Collections.singletonList(key), value);
        lockOwners.remove(key);
        boolean released = Long.valueOf(1L).equals(result);
        if (released) {
            log.debug("释放锁成功: key={}", key);
        } else {
            log.warn("释放锁失败（锁已过期或被他人持有）: key={}", key);
        }
        return released;
    }

    /**
     * Simple lock with auto-generated value.
     */
    public boolean lock(String key, Duration timeout) {
        return tryLock(key, timeout) != null;
    }

    /**
     * Unlock using stored owner value.
     */
    public void unlock(String key) {
        String value = lockOwners.get(key);
        if (value != null) {
            unlock(key, value);
        }
    }
}
