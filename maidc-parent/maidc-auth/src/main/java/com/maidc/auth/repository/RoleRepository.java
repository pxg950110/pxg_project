package com.maidc.auth.repository;

import com.maidc.auth.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    List<RoleEntity> findByIsDeletedFalse();

    Optional<RoleEntity> findByIdAndIsDeletedFalse(Long id);

    boolean existsByRoleCodeAndOrgIdAndIsDeletedFalse(String roleCode, Long orgId);
}
