package com.maidc.model.repository;

import com.maidc.model.entity.ModelMetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ModelMetricRepository extends JpaRepository<ModelMetricEntity, Long> {

    List<ModelMetricEntity> findByDeploymentIdAndCollectedAtBetweenOrderByCollectedAt(Long deploymentId,
                                                                                       LocalDateTime start,
                                                                                       LocalDateTime end);
}
