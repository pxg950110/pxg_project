package com.maidc.data.repository;

import com.maidc.data.entity.FamilyHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FamilyHistoryRepository extends JpaRepository<FamilyHistoryEntity, Long>, JpaSpecificationExecutor<FamilyHistoryEntity> {
}
