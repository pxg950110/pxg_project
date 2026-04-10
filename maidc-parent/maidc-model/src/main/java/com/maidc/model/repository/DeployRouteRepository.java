package com.maidc.model.repository;

import com.maidc.model.entity.DeployRouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeployRouteRepository extends JpaRepository<DeployRouteEntity, Long> {
}
