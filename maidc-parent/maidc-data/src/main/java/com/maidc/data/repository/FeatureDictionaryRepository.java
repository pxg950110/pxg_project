package com.maidc.data.repository;

import com.maidc.data.entity.FeatureDictionaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureDictionaryRepository extends JpaRepository<FeatureDictionaryEntity, Long>,
        JpaSpecificationExecutor<FeatureDictionaryEntity> {
}
