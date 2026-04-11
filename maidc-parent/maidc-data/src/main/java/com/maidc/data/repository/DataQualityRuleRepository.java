package com.maidc.data.repository;

import com.maidc.data.entity.DataQualityRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DataQualityRuleRepository extends JpaRepository<DataQualityRuleEntity, Long>,
        JpaSpecificationExecutor<DataQualityRuleEntity> {
}
