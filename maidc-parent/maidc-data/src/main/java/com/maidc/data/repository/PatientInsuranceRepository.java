package com.maidc.data.repository;

import com.maidc.data.entity.PatientInsuranceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientInsuranceRepository extends JpaRepository<PatientInsuranceEntity, Long>, JpaSpecificationExecutor<PatientInsuranceEntity> {
}
