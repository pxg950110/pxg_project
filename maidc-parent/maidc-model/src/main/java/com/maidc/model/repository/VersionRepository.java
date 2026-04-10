package com.maidc.model.repository;

import com.maidc.model.entity.ModelVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VersionRepository extends JpaRepository<ModelVersionEntity, Long> {

    List<ModelVersionEntity> findByModelIdAndIsDeletedFalseOrderByCreatedAtDesc(Long modelId);

    Optional<ModelVersionEntity> findByIdAndIsDeletedFalse(Long id);
}
