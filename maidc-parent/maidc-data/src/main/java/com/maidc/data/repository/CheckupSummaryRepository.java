package com.maidc.data.repository;

import com.maidc.data.entity.CheckupSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckupSummaryRepository extends JpaRepository<CheckupSummaryEntity, Long>, JpaSpecificationExecutor<CheckupSummaryEntity> {
}
