package com.maidc.model.repository;

import com.maidc.model.entity.ModelVersionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VersionRepository extends JpaRepository<ModelVersionEntity, Long> {

    List<ModelVersionEntity> findByModelIdAndIsDeletedFalseOrderByCreatedAtDesc(Long modelId);

    Page<ModelVersionEntity> findByModelIdAndIsDeletedFalseOrderByCreatedAtDesc(Long modelId, Pageable pageable);

    Optional<ModelVersionEntity> findByIdAndIsDeletedFalse(Long id);

    boolean existsByModelIdAndVersionNoAndIsDeletedFalse(Long modelId, String versionNo);

    long countByModelIdAndIsDeletedFalse(Long modelId);

    Optional<ModelVersionEntity> findFirstByModelIdAndIsDeletedFalseOrderByCreatedAtDesc(Long modelId);
}
