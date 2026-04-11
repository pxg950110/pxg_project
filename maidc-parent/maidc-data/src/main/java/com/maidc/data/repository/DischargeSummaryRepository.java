package com.maidc.data.repository;

import com.maidc.data.entity.DischargeSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DischargeSummaryRepository extends JpaRepository<DischargeSummaryEntity, Long>, JpaSpecificationExecutor<DischargeSummaryEntity> {
}
