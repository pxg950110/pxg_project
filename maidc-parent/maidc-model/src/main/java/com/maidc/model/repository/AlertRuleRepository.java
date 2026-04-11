package com.maidc.model.repository;

import com.maidc.model.entity.AlertRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, Long> {

    List<AlertRuleEntity> findByTargetIdAndIsDeletedFalse(Long targetId);

    List<AlertRuleEntity> findByIsDeletedFalse();

    Optional<AlertRuleEntity> findByIdAndIsDeletedFalse(Long id);
}
