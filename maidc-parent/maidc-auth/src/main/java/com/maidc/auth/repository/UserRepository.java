package com.maidc.auth.repository;

import com.maidc.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByUsernameAndOrgIdAndIsDeletedFalse(String username, Long orgId);

    boolean existsByUsernameAndOrgIdAndIsDeletedFalse(String username, Long orgId);

    Optional<UserEntity> findByIdAndIsDeletedFalse(Long id);
}
