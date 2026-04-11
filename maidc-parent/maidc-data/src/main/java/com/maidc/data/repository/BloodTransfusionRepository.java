package com.maidc.data.repository;

import com.maidc.data.entity.BloodTransfusionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BloodTransfusionRepository extends JpaRepository<BloodTransfusionEntity, Long>, JpaSpecificationExecutor<BloodTransfusionEntity> {
}
