package com.maidc.data.repository;

import com.maidc.data.entity.FeeRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FeeRecordRepository extends JpaRepository<FeeRecordEntity, Long>, JpaSpecificationExecutor<FeeRecordEntity> {
}
