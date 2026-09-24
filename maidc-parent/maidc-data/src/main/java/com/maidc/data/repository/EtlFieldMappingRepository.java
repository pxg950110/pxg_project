package com.maidc.data.repository;

import com.maidc.data.entity.EtlFieldMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtlFieldMappingRepository extends JpaRepository<EtlFieldMappingEntity, Long> {

    List<EtlFieldMappingEntity> findByStepIdAndIsDeletedFalseOrderBySortOrder(Long stepId);

    List<EtlFieldMappingEntity> findByStepIdInAndIsDeletedFalseOrderByStepIdAscSortOrderAsc(List<Long> stepIds);

    void deleteByStepId(Long stepId);
}
