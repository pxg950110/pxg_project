package com.maidc.data.repository;

import com.maidc.data.entity.HealthCheckupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface HealthCheckupRepository extends JpaRepository<HealthCheckupEntity, Long>, JpaSpecificationExecutor<HealthCheckupEntity> {
}
