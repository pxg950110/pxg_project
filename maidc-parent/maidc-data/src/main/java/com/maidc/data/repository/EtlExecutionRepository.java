package com.maidc.data.repository;

import com.maidc.data.entity.EtlExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EtlExecutionRepository extends JpaRepository<EtlExecutionEntity, Long>,
        JpaSpecificationExecutor<EtlExecutionEntity> {

    Page<EtlExecutionEntity> findByPipelineIdAndIsDeletedFalse(Long pipelineId, Pageable pageable);

    List<EtlExecutionEntity> findByPipelineIdAndStatusAndIsDeletedFalse(Long pipelineId, String status);
}
