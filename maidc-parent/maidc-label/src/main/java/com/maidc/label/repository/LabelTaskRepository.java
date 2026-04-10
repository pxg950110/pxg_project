package com.maidc.label.repository;

import com.maidc.label.entity.LabelTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LabelTaskRepository extends JpaRepository<LabelTaskEntity, String>,
        JpaSpecificationExecutor<LabelTaskEntity> {
}
