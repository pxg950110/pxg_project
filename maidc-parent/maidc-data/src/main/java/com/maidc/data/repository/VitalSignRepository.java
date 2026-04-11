package com.maidc.data.repository;

import com.maidc.data.entity.VitalSignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VitalSignRepository extends JpaRepository<VitalSignEntity, Long>, JpaSpecificationExecutor<VitalSignEntity> {
}
