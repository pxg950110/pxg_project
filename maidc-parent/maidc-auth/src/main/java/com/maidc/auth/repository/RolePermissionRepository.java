package com.maidc.auth.repository;

import com.maidc.auth.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, Long> {

    List<RolePermissionEntity> findByRoleId(Long roleId);

    List<RolePermissionEntity> findByRoleIdIn(List<Long> roleIds);

    void deleteByRoleId(Long roleId);
}
