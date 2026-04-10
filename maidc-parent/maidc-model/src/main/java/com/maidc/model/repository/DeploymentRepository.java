package com.maidc.model.repository;

import com.maidc.model.entity.DeploymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeploymentRepository extends JpaRepository<DeploymentEntity, Long> {

    List<DeploymentEntity> findByStatusAndIsDeletedFalse(String status);
}
