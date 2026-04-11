package com.maidc.auth.service;

import com.maidc.auth.dto.UserCreateDTO;
import com.maidc.auth.dto.UserUpdateDTO;
import com.maidc.auth.entity.UserEntity;
import com.maidc.auth.entity.UserRoleEntity;
import com.maidc.auth.repository.UserRepository;
import com.maidc.auth.repository.UserRoleRepository;
import com.maidc.auth.repository.RoleRepository;
import com.maidc.auth.vo.UserDetailVO;
import com.maidc.auth.vo.UserVO;
import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserVO> listUsers(int page, int pageSize, String keyword, String status) {
        Specification<UserEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.or(
                        cb.like(root.get("username"), "%" + keyword + "%"),
                        cb.like(root.get("realName"), "%" + keyword + "%")
                ));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserEntity> pageResult = userRepository.findAll(spec, PageRequest.of(page - 1, pageSize));

        List<UserVO> items = pageResult.getContent().stream().map(user -> {
            List<String> roleNames = getUserRoleNames(user.getId());
            return UserVO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .realName(user.getRealName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .status(user.getStatus())
                    .roleNames(roleNames)
                    .orgId(user.getOrgId())
                    .createdAt(user.getCreatedAt())
                    .lastLogin(user.getLastLoginAt())
                    .mustChangePwd(user.getMustChangePwd())
                    .build();
        }).toList();

        return PageResult.of(pageResult.map(e -> {
            int idx = pageResult.getContent().indexOf(e);
            return items.get(idx >= 0 ? idx : 0);
        }));
    }

    @Transactional
    public UserVO createUser(UserCreateDTO dto) {
        if (userRepository.existsByUsernameAndOrgIdAndIsDeletedFalse(dto.getUsername(), dto.getOrgId())) {
            throw new BusinessException(ErrorCode.MODEL_CODE_DUPLICATE);
        }

        UserEntity user = new UserEntity();
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setOrgId(dto.getOrgId());
        user.setMustChangePwd(dto.getMustChangePwd() != null ? dto.getMustChangePwd() : false);
        user.setPasswordChangedAt(LocalDateTime.now());
        user = userRepository.save(user);

        // Assign roles
        if (dto.getRoleIds() != null) {
            for (Long roleId : dto.getRoleIds()) {
                UserRoleEntity ur = new UserRoleEntity();
                ur.setUserId(user.getId());
                ur.setRoleId(roleId);
                ur.setOrgId(dto.getOrgId());
                userRoleRepository.save(ur);
            }
        }

        log.info("用户创建成功: username={}", user.getUsername());

        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .orgId(user.getOrgId())
                .createdAt(user.getCreatedAt())
                .mustChangePwd(user.getMustChangePwd())
                .build();
    }

    public UserDetailVO getUser(Long id) {
        UserEntity user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(user.getId());
        List<Long> roleIds = userRoles.stream().map(UserRoleEntity::getRoleId).toList();

        var roleVOs = roleRepository.findAllById(roleIds).stream()
                .map(role -> com.maidc.auth.vo.RoleVO.builder()
                        .id(role.getId())
                        .code(role.getRoleCode())
                        .name(role.getRoleName())
                        .description(role.getDescription())
                        .build())
                .toList();

        return UserDetailVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .roles(roleVOs)
                .orgId(user.getOrgId())
                .lastLogin(user.getLastLoginAt())
                .passwordChangedAt(user.getPasswordChangedAt())
                .mustChangePwd(user.getMustChangePwd())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional
    public UserVO updateUser(Long id, UserUpdateDTO dto) {
        UserEntity user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (dto.getRealName() != null) user.setRealName(dto.getRealName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        if (dto.getOrgId() != null) user.setOrgId(dto.getOrgId());

        user = userRepository.save(user);

        if (dto.getRoleIds() != null) {
            userRoleRepository.deleteByUserId(user.getId());
            for (Long roleId : dto.getRoleIds()) {
                UserRoleEntity ur = new UserRoleEntity();
                ur.setUserId(user.getId());
                ur.setRoleId(roleId);
                ur.setOrgId(user.getOrgId());
                userRoleRepository.save(ur);
            }
        }

        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .roleNames(getUserRoleNames(user.getId()))
                .orgId(user.getOrgId())
                .build();
    }

    @Transactional
    public void resetPassword(Long id, String currentPassword, String newPassword) {
        UserEntity user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePwd(false);
        userRepository.save(user);

        log.info("密码重置成功: userId={}", id);
    }

    private List<String> getUserRoleNames(Long userId) {
        List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(userId);
        List<Long> roleIds = userRoles.stream().map(UserRoleEntity::getRoleId).toList();
        if (roleIds.isEmpty()) return List.of();
        return roleRepository.findAllById(roleIds).stream()
                .map(r -> r.getRoleName())
                .toList();
    }
}
