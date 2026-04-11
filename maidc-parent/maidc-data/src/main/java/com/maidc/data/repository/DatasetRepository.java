package com.maidc.data.repository;

import com.maidc.data.entity.DatasetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatasetRepository extends JpaRepository<DatasetEntity, Long>, JpaSpecificationExecutor<DatasetEntity> {

    Optional<DatasetEntity> findByIdAndIsDeletedFalse(Long id);

    List<DatasetEntity> findByProjectIdAndIsDeletedFalse(Long projectId);
}
