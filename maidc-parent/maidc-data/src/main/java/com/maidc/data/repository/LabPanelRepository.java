package com.maidc.data.repository;

import com.maidc.data.entity.LabPanelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LabPanelRepository extends JpaRepository<LabPanelEntity, Long>, JpaSpecificationExecutor<LabPanelEntity> {
}
