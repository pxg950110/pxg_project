package com.maidc.data.repository;

import com.maidc.data.entity.PatientBedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientBedRepository extends JpaRepository<PatientBedEntity, Long>, JpaSpecificationExecutor<PatientBedEntity> {
}
