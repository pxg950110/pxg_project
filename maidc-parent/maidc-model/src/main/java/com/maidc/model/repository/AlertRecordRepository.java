package com.maidc.model.repository;

import com.maidc.model.entity.AlertRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecordEntity, Long> {

    Page<AlertRecordEntity> findByRuleIdOrderByTriggeredAtDesc(Long ruleId, Pageable pageable);

    Page<AlertRecordEntity> findByStatusOrderByTriggeredAtDesc(String status, Pageable pageable);
}
