package com.maidc.data.repository;

import com.maidc.data.entity.CheckupComparisonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckupComparisonRepository extends JpaRepository<CheckupComparisonEntity, Long>, JpaSpecificationExecutor<CheckupComparisonEntity> {
}
