package com.maidc.data.repository;

import com.maidc.data.entity.EtlTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EtlTaskRepository extends JpaRepository<EtlTaskEntity, Long>, JpaSpecificationExecutor<EtlTaskEntity> {

    Optional<EtlTaskEntity> findByIdAndIsDeletedFalse(Long id);
}
