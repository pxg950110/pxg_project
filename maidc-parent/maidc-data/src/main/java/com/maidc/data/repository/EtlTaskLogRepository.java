package com.maidc.data.repository;

import com.maidc.data.entity.EtlTaskLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface EtlTaskLogRepository extends JpaRepository<EtlTaskLogEntity, Long>,
        JpaSpecificationExecutor<EtlTaskLogEntity> {
}
