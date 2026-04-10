package com.maidc.audit.repository;

import com.maidc.audit.entity.DataAccessLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DataAccessLogRepository extends JpaRepository<DataAccessLogEntity, String>, JpaSpecificationExecutor<DataAccessLogEntity> {
}
