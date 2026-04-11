package com.maidc.auth.service;

import com.maidc.auth.dto.LoginDTO;
import com.maidc.auth.dto.RefreshTokenDTO;
import com.maidc.auth.entity.RoleEntity;
import com.maidc.auth.entity.UserEntity;
import com.maidc.auth.entity.UserRoleEntity;
import com.maidc.auth.repository.RoleRepository;
import com.maidc.auth.repository.UserRepository;
import com.maidc.auth.repository.UserRoleRepository;
import com.maidc.auth.vo.LoginVO;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.security.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private UserEntity activeUser;
    private LoginDTO loginDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessExpiration", 7200000L);

        activeUser = new UserEntity();
        activeUser.setId(1L);
        activeUser.setUsername("admin");
        activeUser.setPasswordHash("$2a$10$encodedhash");
        activeUser.setRealName("管理员");
        activeUser.setStatus("ACTIVE");
        activeUser.setOrgId(0L);

        loginDTO = new LoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("password123");
    }

    // ==================== login tests ====================

    @Test
    void login_validCredentials_returnsLoginVO() {
        // Arrange
        when(userRepository.findByUsernameAndOrgIdAndIsDeletedFalse("admin", 0L))
                .thenReturn(Optional.of(activeUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("maidc:auth:attempts:1")).thenReturn(null);
        when(passwordEncoder.matches("password123", "$2a$10$encodedhash")).thenReturn(true);

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(1L);
        userRole.setRoleId(10L);
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(userRole));

        RoleEntity role = new RoleEntity();
        role.setId(10L);
        role.setRoleCode("ADMIN");
        when(roleRepository.findAllById(List.of(10L))).thenReturn(List.of(role));

        when(jwtUtils.generateAccessToken(1L, "admin", List.of("ADMIN"), 0L)).thenReturn("access-token");
        when(jwtUtils.generateRefreshToken(1L, "admin")).thenReturn("refresh-token");
        when(userRepository.save(activeUser)).thenReturn(activeUser);

        // Act
        LoginVO result = authService.login(loginDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getUsername()).isEqualTo("admin");
        assertThat(result.getUser().getRoles()).containsExactly("ADMIN");

        verify(redisTemplate).delete("maidc:auth:attempts:1");
        verify(userRepository).save(activeUser);
    }

    @Test
    void login_nonExistentUser_throwsUnauthorized() {
        // Arrange
        when(userRepository.findByUsernameAndOrgIdAndIsDeletedFalse("admin", 0L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginDTO))
                .isInstanceOf(BusinessException.class);

        verify(userRepository).findByUsernameAndOrgIdAndIsDeletedFalse("admin", 0L);
        verifyNoInteractions(passwordEncoder, redisTemplate, jwtUtils);
    }

    @Test
    void login_disabledUser_throwsDisabledException() {
        // Arrange
        activeUser.setStatus("DISABLED");
        when(userRepository.findByUsernameAndOrgIdAndIsDeletedFalse("admin", 0L))
                .thenReturn(Optional.of(activeUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("maidc:auth:attempts:1")).thenReturn(null);
        when(passwordEncoder.matches("password123", "$2a$10$encodedhash")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("禁用");

        verify(jwtUtils, never()).generateAccessToken(anyLong(), anyString(), anyList(), anyLong());
    }

    @Test
    void login_wrongPassword_incrementsAttemptsAndThrows() {
        // Arrange
        when(userRepository.findByUsernameAndOrgIdAndIsDeletedFalse("admin", 0L))
                .thenReturn(Optional.of(activeUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("maidc:auth:attempts:1")).thenReturn(null);
        when(passwordEncoder.matches("password123", "$2a$10$encodedhash")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");

        verify(redisTemplate.opsForValue()).increment("maidc:auth:attempts:1");
        verify(jwtUtils, never()).generateAccessToken(anyLong(), anyString(), anyList(), anyLong());
    }

    @Test
    void login_lockedAccount_throwsLockedException() {
        // Arrange
        when(userRepository.findByUsernameAndOrgIdAndIsDeletedFalse("admin", 0L))
                .thenReturn(Optional.of(activeUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("maidc:auth:attempts:1")).thenReturn("5");

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("锁定");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_successWithNoRoles_returnsEmptyRoleList() {
        // Arrange
        when(userRepository.findByUsernameAndOrgIdAndIsDeletedFalse("admin", 0L))
                .thenReturn(Optional.of(activeUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("maidc:auth:attempts:1")).thenReturn(null);
        when(passwordEncoder.matches("password123", "$2a$10$encodedhash")).thenReturn(true);
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());
        when(jwtUtils.generateAccessToken(1L, "admin", List.of(), 0L)).thenReturn("access-token");
        when(jwtUtils.generateRefreshToken(1L, "admin")).thenReturn("refresh-token");
        when(userRepository.save(activeUser)).thenReturn(activeUser);

        // Act
        LoginVO result = authService.login(loginDTO);

        // Assert
        assertThat(result.getUser().getRoles()).isEmpty();
        // AuthService still calls findAllById with empty list when no roles exist
        verify(roleRepository).findAllById(List.of());
    }

    // ==================== logout tests ====================

    @Test
    void logout_withBearerToken_addsToBlacklist() {
        // Arrange
        when(jwtUtils.getUserIdFromToken("test-token")).thenReturn(1L);

        // Act
        authService.logout("Bearer test-token");

        // Assert
        verify(jwtUtils).addToBlacklist(eq("test-token"), any());
    }

    @Test
    void logout_withNullToken_doesNothing() {
        // Act
        authService.logout(null);

        // Assert
        verifyNoInteractions(jwtUtils);
    }

    // ==================== refreshToken tests ====================

    @Test
    void refreshToken_invalidToken_throwsUnauthorized() {
        // Arrange
        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setRefreshToken("invalid-token");
        when(jwtUtils.validateToken("invalid-token")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(dto))
                .isInstanceOf(BusinessException.class);

        verify(jwtUtils).validateToken("invalid-token");
    }
}
