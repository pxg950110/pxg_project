package com.maidc.data.repository;

import com.maidc.data.entity.PatientContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientContactRepository extends JpaRepository<PatientContactEntity, Long>, JpaSpecificationExecutor<PatientContactEntity> {
}
