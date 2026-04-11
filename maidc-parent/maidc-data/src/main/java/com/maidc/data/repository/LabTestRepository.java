package com.maidc.data.repository;

import com.maidc.data.entity.LabTestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LabTestRepository extends JpaRepository<LabTestEntity, Long>, JpaSpecificationExecutor<LabTestEntity> {
}
