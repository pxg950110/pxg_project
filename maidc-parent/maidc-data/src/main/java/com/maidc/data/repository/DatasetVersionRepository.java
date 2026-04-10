package com.maidc.data.repository;

import com.maidc.data.entity.DatasetVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatasetVersionRepository extends JpaRepository<DatasetVersionEntity, Long> {

    List<DatasetVersionEntity> findByDatasetIdAndIsDeletedFalseOrderByVersionNoDesc(Long datasetId);

    Optional<DatasetVersionEntity> findFirstByDatasetIdAndIsDeletedFalseOrderByCreatedAtDesc(Long datasetId);
}
