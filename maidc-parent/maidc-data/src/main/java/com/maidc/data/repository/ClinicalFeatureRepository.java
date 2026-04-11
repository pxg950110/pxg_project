package com.maidc.data.repository;

import com.maidc.data.entity.ClinicalFeatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ClinicalFeatureRepository extends JpaRepository<ClinicalFeatureEntity, Long>,
        JpaSpecificationExecutor<ClinicalFeatureEntity> {
}
