package com.maidc.audit.repository;

import com.maidc.audit.entity.SystemEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemEventRepository extends JpaRepository<SystemEventEntity, String>, JpaSpecificationExecutor<SystemEventEntity> {
}
