package com.maidc.data.repository;

import com.maidc.data.entity.EtlStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtlStepRepository extends JpaRepository<EtlStepEntity, Long> {

    List<EtlStepEntity> findByPipelineIdAndIsDeletedFalseOrderByStepOrder(Long pipelineId);

    long countByPipelineIdAndIsDeletedFalse(Long pipelineId);

    void deleteByPipelineId(Long pipelineId);
}
