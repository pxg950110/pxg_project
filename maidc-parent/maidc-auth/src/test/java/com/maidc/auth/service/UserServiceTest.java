package com.maidc.auth.service;

import com.maidc.auth.dto.UserCreateDTO;
import com.maidc.auth.dto.UserUpdateDTO;
import com.maidc.auth.entity.UserEntity;
import com.maidc.auth.entity.UserRoleEntity;
import com.maidc.auth.repository.RoleRepository;
import com.maidc.auth.repository.UserRepository;
import com.maidc.auth.repository.UserRoleRepository;
import com.maidc.auth.vo.UserDetailVO;
import com.maidc.auth.vo.UserVO;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserEntity testUser;
    private UserCreateDTO createDTO;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPasswordHash("$2a$10$encodedhash");
        testUser.setRealName("测试用户");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setStatus("ACTIVE");
        testUser.setOrgId(1L);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setMustChangePwd(false);

        createDTO = new UserCreateDTO();
        createDTO.setUsername("newuser");
        createDTO.setPassword("password123");
        createDTO.setRealName("新用户");
        createDTO.setEmail("new@example.com");
        createDTO.setPhone("13900139000");
        createDTO.setOrgId(1L);
        createDTO.setRoleIds(List.of(1L, 2L));
        createDTO.setMustChangePwd(false);
    }

    // ==================== createUser tests ====================

    @Test
    void createUser_duplicateUsername_throwsBusinessException() {
        // Arrange
        when(userRepository.existsByUsernameAndOrgIdAndIsDeletedFalse("newuser", 1L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(createDTO))
                .isInstanceOf(BusinessException.class);

        verify(userRepository).existsByUsernameAndOrgIdAndIsDeletedFalse("newuser", 1L);
        verify(userRepository, never()).save(any(UserEntity.class));
        verifyNoInteractions(passwordEncoder, userRoleRepository);
    }

    @Test
    void createUser_validInput_returnsUserVO() {
        // Arrange
        when(userRepository.existsByUsernameAndOrgIdAndIsDeletedFalse("newuser", 1L)).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$newhash");

        UserEntity savedUser = new UserEntity();
        savedUser.setId(2L);
        savedUser.setUsername("newuser");
        savedUser.setRealName("新用户");
        savedUser.setEmail("new@example.com");
        savedUser.setPhone("13900139000");
        savedUser.setStatus("ACTIVE");
        savedUser.setOrgId(1L);
        savedUser.setCreatedAt(LocalDateTime.now());
        savedUser.setMustChangePwd(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        // Act
        UserVO result = userService.createUser(createDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getRealName()).isEqualTo("新用户");

        verify(passwordEncoder).encode("password123");
        // Two role assignments
        verify(userRoleRepository, times(2)).save(any(UserRoleEntity.class));
    }

    @Test
    void createUser_noRoleIds_doesNotSaveRoles() {
        // Arrange
        createDTO.setRoleIds(null);
        when(userRepository.existsByUsernameAndOrgIdAndIsDeletedFalse("newuser", 1L)).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$newhash");

        UserEntity savedUser = new UserEntity();
        savedUser.setId(2L);
        savedUser.setUsername("newuser");
        savedUser.setRealName("新用户");
        savedUser.setEmail("new@example.com");
        savedUser.setPhone("13900139000");
        savedUser.setStatus("ACTIVE");
        savedUser.setOrgId(1L);
        savedUser.setCreatedAt(LocalDateTime.now());
        savedUser.setMustChangePwd(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        // Act
        UserVO result = userService.createUser(createDTO);

        // Assert
        assertThat(result).isNotNull();
        verifyNoInteractions(userRoleRepository);
    }

    // ==================== getUser tests ====================

    @Test
    void getUser_nonExistentId_throwsBusinessException() {
        // Arrange
        when(userRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUser(999L))
                .isInstanceOf(BusinessException.class);

        verify(userRepository).findByIdAndIsDeletedFalse(999L);
    }

    @Test
    void getUser_validId_returnsUserDetailVO() {
        // Arrange
        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testUser));

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(1L);
        userRole.setRoleId(10L);
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(userRole));

        var role = new com.maidc.auth.entity.RoleEntity();
        role.setId(10L);
        role.setRoleCode("DOCTOR");
        role.setRoleName("医生");
        role.setDescription("医生角色");
        when(roleRepository.findAllById(List.of(10L))).thenReturn(List.of(role));

        // Act
        UserDetailVO result = userService.getUser(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getRoles()).hasSize(1);
        assertThat(result.getRoles().get(0).getCode()).isEqualTo("DOCTOR");
    }

    // ==================== updateUser tests ====================

    @Test
    void updateUser_nonExistentId_throwsBusinessException() {
        // Arrange
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        when(userRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(999L, updateDTO))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateUser_validInput_returnsUpdatedUserVO() {
        // Arrange
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setRealName("更新名称");
        updateDTO.setEmail("updated@example.com");
        updateDTO.setRoleIds(List.of(3L));

        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());

        // Act
        UserVO result = userService.updateUser(1L, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(testUser.getRealName()).isEqualTo("更新名称");
        assertThat(testUser.getEmail()).isEqualTo("updated@example.com");
        verify(userRoleRepository).deleteByUserId(1L);
        verify(userRoleRepository).save(any(UserRoleEntity.class));
    }

    // ==================== resetPassword tests ====================

    @Test
    void resetPassword_wrongCurrentPassword_throwsException() {
        // Arrange
        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpwd", "$2a$10$encodedhash")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.resetPassword(1L, "wrongpwd", "newpassword"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码不正确");

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void resetPassword_correctCurrentPassword_succeeds() {
        // Arrange
        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldpwd", "$2a$10$encodedhash")).thenReturn(true);
        when(passwordEncoder.encode("newpassword")).thenReturn("$2a$10$newhash");
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        // Act
        userService.resetPassword(1L, "oldpwd", "newpassword");

        // Assert
        assertThat(testUser.getPasswordHash()).isEqualTo("$2a$10$newhash");
        assertThat(testUser.getMustChangePwd()).isFalse();
        assertThat(testUser.getPasswordChangedAt()).isNotNull();
        verify(userRepository).save(testUser);
    }

    // ==================== listUsers tests ====================

    @Test
    @SuppressWarnings("unchecked")
    void listUsers_returnsPagedResults() {
        // Arrange
        Page<UserEntity> page = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());

        // Act
        PageResult<UserVO> result = userService.listUsers(1, 10, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
    }

    // ==================== edge case: soft delete flag ====================

    @Test
    void getUser_deletedUser_throwsNotFound() {
        // findByIdAndIsDeletedFalse only returns non-deleted users
        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(1L))
                .isInstanceOf(BusinessException.class);
    }
}
