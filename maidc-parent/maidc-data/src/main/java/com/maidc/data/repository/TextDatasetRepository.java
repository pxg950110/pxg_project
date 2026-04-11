package com.maidc.data.repository;

import com.maidc.data.entity.TextDatasetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TextDatasetRepository extends JpaRepository<TextDatasetEntity, Long>,
        JpaSpecificationExecutor<TextDatasetEntity> {
}
