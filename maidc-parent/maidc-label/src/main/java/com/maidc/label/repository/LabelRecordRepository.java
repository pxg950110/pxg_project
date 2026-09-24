package com.maidc.label.repository;

import com.maidc.label.entity.LabelRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabelRecordRepository extends JpaRepository<LabelRecordEntity, Long>,
        JpaSpecificationExecutor<LabelRecordEntity> {

    List<LabelRecordEntity> findByTaskIdAndIsDeletedFalse(String taskId);

    Optional<LabelRecordEntity> findByIdAndIsDeletedFalse(Long id);

    long countByTaskIdAndIsDeletedFalse(String taskId);

    long countByTaskIdAndVerificationStatusAndIsDeletedFalse(String taskId, String verificationStatus);
}
