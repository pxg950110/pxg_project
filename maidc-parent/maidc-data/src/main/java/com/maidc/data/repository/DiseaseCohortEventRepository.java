package com.maidc.data.repository;

import com.maidc.data.entity.DiseaseCohortEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiseaseCohortEventRepository extends JpaRepository<DiseaseCohortEventEntity, Long> {

    /** 工作台队列动态（FR6）：最近 N 条，created_at DESC */
    List<DiseaseCohortEventEntity> findTop3ByOrgIdAndIsDeletedFalseOrderByCreatedAtDesc(Long orgId);
}
