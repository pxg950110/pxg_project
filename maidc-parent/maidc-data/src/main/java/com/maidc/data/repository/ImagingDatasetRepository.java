package com.maidc.data.repository;

import com.maidc.data.entity.ImagingDatasetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagingDatasetRepository extends JpaRepository<ImagingDatasetEntity, Long>,
        JpaSpecificationExecutor<ImagingDatasetEntity> {
}
