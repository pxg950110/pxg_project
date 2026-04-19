package com.maidc.data.repository;

import com.maidc.data.entity.EtlPipelineEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EtlPipelineRepository extends JpaRepository<EtlPipelineEntity, Long>,
        JpaSpecificationExecutor<EtlPipelineEntity> {

    Page<EtlPipelineEntity> findBySourceIdAndIsDeletedFalse(Long sourceId, Pageable pageable);

    List<EtlPipelineEntity> findByStatusAndIsDeletedFalse(String status);
}
