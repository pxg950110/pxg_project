package com.maidc.data.repository;

import com.maidc.data.entity.DiseaseCohortEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DiseaseCohortRepository extends JpaRepository<DiseaseCohortEntity, Long>, JpaSpecificationExecutor<DiseaseCohortEntity> {
}
