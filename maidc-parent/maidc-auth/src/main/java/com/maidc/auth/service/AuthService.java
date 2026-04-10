package com.maidc.auth.service;

import com.maidc.auth.dto.LoginDTO;
import com.maidc.auth.dto.RefreshTokenDTO;
import com.maidc.auth.entity.*;
import com.maidc.auth.repository.*;
import com.maidc.auth.vo.LoginVO;
import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.security.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Value("${maidc.jwt.access-expiration:7200000}")
    private long accessExpiration;

    private static final String LOGIN_ATTEMPTS_PREFIX = "maidc:auth:attempts:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);

    @Transactional
    public LoginVO login(LoginDTO dto) {
        UserEntity user = userRepository.findByUsernameAndOrgIdAndIsDeletedFalse(dto.getUsername(), 0L)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        // Check lockout
        String attemptsKey = LOGIN_ATTEMPTS_PREFIX + user.getId();
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= MAX_ATTEMPTS) {
            throw new BusinessException(401, "账号已锁定，请30分钟后重试");
        }

        // Verify password
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, LOCK_DURATION);
            throw new BusinessException(401, "用户名或密码错误");
        }

        // Check status
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(401, "账号已被禁用");
        }

        // Clear attempts on success
        redisTemplate.delete(attemptsKey);

        // Get roles
        List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(user.getId());
        List<Long> roleIds = userRoles.stream().map(UserRoleEntity::getRoleId).toList();
        List<String> roleCodes = roleRepository.findAllById(roleIds).stream()
                .map(RoleEntity::getRoleCode).toList();

        // Generate tokens
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), roleCodes, user.getOrgId());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("用户登录成功: username={}", user.getUsername());

        return LoginVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessExpiration / 1000)
                .user(LoginVO.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .realName(user.getRealName())
                        .roles(roleCodes)
                        .orgId(user.getOrgId())
                        .build())
                .build();
    }

    public LoginVO refreshToken(RefreshTokenDTO dto) {
        if (!jwtUtils.validateToken(dto.getRefreshToken())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = jwtUtils.getUserIdFromToken(dto.getRefreshToken());
        String username = jwtUtils.getUsernameFromToken(dto.getRefreshToken());

        // Get roles for new access token
        List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(userId);
        List<Long> roleIds = userRoles.stream().map(UserRoleEntity::getRoleId).toList();
        List<String> roleCodes = roleRepository.findAllById(roleIds).stream()
                .map(RoleEntity::getRoleCode).toList();

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        String accessToken = jwtUtils.generateAccessToken(userId, username, roleCodes, user.getOrgId());

        return LoginVO.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(accessExpiration / 1000)
                .build();
    }

    @Transactional
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null) {
            jwtUtils.addToBlacklist(token, Duration.ofHours(2));
            log.info("用户登出: userId={}", jwtUtils.getUserIdFromToken(token));
        }
    }
}
