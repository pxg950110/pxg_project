package com.maidc.data.repository;

import com.maidc.data.entity.AllergyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AllergyRepository extends JpaRepository<AllergyEntity, Long>, JpaSpecificationExecutor<AllergyEntity> {
}
