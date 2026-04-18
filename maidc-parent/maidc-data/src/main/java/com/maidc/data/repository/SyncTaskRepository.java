package com.maidc.data.repository;

import com.maidc.data.entity.SyncTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncTaskRepository extends JpaRepository<SyncTaskEntity, Long>,
        JpaSpecificationExecutor<SyncTaskEntity> {

    List<SyncTaskEntity> findBySourceIdOrderByCreatedAtDesc(Long sourceId);
}
