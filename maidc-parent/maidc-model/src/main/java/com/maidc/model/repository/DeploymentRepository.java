package com.maidc.model.repository;

import com.maidc.model.entity.DeploymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeploymentRepository extends JpaRepository<DeploymentEntity, Long> {

    Optional<DeploymentEntity> findByIdAndIsDeletedFalse(Long id);

    List<DeploymentEntity> findByStatusAndIsDeletedFalse(String status);
}
