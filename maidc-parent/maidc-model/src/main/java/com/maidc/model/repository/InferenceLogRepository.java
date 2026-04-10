package com.maidc.model.repository;

import com.maidc.model.entity.InferenceLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface InferenceLogRepository extends JpaRepository<InferenceLogEntity, Long> {

    Page<InferenceLogEntity> findByDeploymentIdAndCreatedAtBetween(Long deploymentId,
                                                                    LocalDateTime start,
                                                                    LocalDateTime end,
                                                                    Pageable pageable);
}
