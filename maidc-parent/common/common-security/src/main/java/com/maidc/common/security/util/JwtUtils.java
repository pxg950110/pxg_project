package com.maidc.common.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtUtils {

    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;
    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "maidc:auth:token:blacklist:";

    public JwtUtils(
            @Value("${maidc.jwt.secret:maidc-secret-key-for-jwt-token-generation-must-be-at-least-256-bits}") String secret,
            @Value("${maidc.jwt.access-expiration:7200000}") long accessExpiration,
            @Value("${maidc.jwt.refresh-expiration:604800000}") long refreshExpiration,
            StringRedisTemplate redisTemplate) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.redisTemplate = redisTemplate;
    }

    public String generateAccessToken(Long userId, String username, List<String> roles, Long orgId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        "userId", userId,
                        "roles", roles,
                        "orgId", orgId
                ))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpiration))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claims(Map.of("userId", userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiration))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            if (isBlacklisted(token)) {
                return false;
            }
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.debug("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        Object roles = claims.get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public Long getOrgIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object orgId = claims.get("orgId");
        if (orgId instanceof Number) {
            return ((Number) orgId).longValue();
        }
        return null;
    }

    public void addToBlacklist(String token, Duration ttl) {
        String jti = parseToken(token).getId();
        String key = BLACKLIST_PREFIX + (jti != null ? jti : token.hashCode());
        redisTemplate.opsForValue().set(key, "1", ttl);
    }

    public boolean isBlacklisted(String token) {
        try {
            Claims claims = parseToken(token);
            String jti = claims.getId();
            String key = BLACKLIST_PREFIX + (jti != null ? jti : token.hashCode());
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            return false;
        }
    }
}
