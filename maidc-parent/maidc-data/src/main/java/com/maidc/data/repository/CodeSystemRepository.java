package com.maidc.data.repository;

import com.maidc.data.entity.CodeSystemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodeSystemRepository extends JpaRepository<CodeSystemEntity, Long>, JpaSpecificationExecutor<CodeSystemEntity> {

    Optional<CodeSystemEntity> findByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIsDeletedFalse(String code);
}
