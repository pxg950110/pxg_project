package com.maidc.data.repository;

import com.maidc.data.entity.DataQualityResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DataQualityResultRepository extends JpaRepository<DataQualityResultEntity, Long>,
        JpaSpecificationExecutor<DataQualityResultEntity> {
}
