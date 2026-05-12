package com.maidc.data.repository;

import com.maidc.data.entity.OperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationRepository extends JpaRepository<OperationEntity, Long>, JpaSpecificationExecutor<OperationEntity> {
    List<OperationEntity> findByEncounterIdAndIsDeletedFalse(Long encounterId);
}
