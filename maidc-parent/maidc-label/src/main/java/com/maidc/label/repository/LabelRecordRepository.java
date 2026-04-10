package com.maidc.label.repository;

import com.maidc.label.entity.LabelRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabelRecordRepository extends JpaRepository<LabelRecordEntity, String>,
        JpaSpecificationExecutor<LabelRecordEntity> {

    List<LabelRecordEntity> findByTaskIdAndIsDeletedFalse(String taskId);

    long countByTaskIdAndIsDeletedFalse(String taskId);

    long countByTaskIdAndVerificationStatusAndIsDeletedFalse(String taskId, String verificationStatus);
}
