package com.maidc.data.repository;

import com.maidc.data.entity.GenomicDatasetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GenomicDatasetRepository extends JpaRepository<GenomicDatasetEntity, Long>,
        JpaSpecificationExecutor<GenomicDatasetEntity> {
}
