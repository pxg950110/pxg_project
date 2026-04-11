package com.maidc.data.repository;

import com.maidc.data.entity.DatasetAccessLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetAccessLogRepository extends JpaRepository<DatasetAccessLogEntity, Long>,
        JpaSpecificationExecutor<DatasetAccessLogEntity> {
}
