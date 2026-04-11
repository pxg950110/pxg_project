package com.maidc.data.repository;

import com.maidc.data.entity.CheckupItemResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckupItemResultRepository extends JpaRepository<CheckupItemResultEntity, Long>, JpaSpecificationExecutor<CheckupItemResultEntity> {
}
