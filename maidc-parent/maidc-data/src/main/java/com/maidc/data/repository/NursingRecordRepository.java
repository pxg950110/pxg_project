package com.maidc.data.repository;

import com.maidc.data.entity.NursingRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface NursingRecordRepository extends JpaRepository<NursingRecordEntity, Long>, JpaSpecificationExecutor<NursingRecordEntity> {
}
