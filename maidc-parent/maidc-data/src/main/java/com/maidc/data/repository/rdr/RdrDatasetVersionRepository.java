package com.maidc.data.repository.rdr;

import com.maidc.data.entity.rdr.RdrDatasetVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RDR数据集版本Repository
 */
@Repository
public interface RdrDatasetVersionRepository extends
        JpaRepository<RdrDatasetVersionEntity, Long>,
        JpaSpecificationExecutor<RdrDatasetVersionEntity> {

    Optional<RdrDatasetVersionEntity> findByVersionCodeAndIsDeletedFalse(String versionCode);

    List<RdrDatasetVersionEntity> findByDatasetIdAndIsDeletedFalseOrderByCreatedAtDesc(Long datasetId);

    Optional<RdrDatasetVersionEntity> findByDatasetIdAndIsCurrentAndIsDeletedFalse(Long datasetId, Boolean isCurrent);

    boolean existsByVersionCodeAndIsDeletedFalse(String versionCode);

    @Modifying
    @Query("UPDATE RdrDatasetVersionEntity v SET v.isCurrent = false WHERE v.datasetId = ?1")
    void clearCurrentFlag(Long datasetId);

    @Modifying
    @Query("UPDATE RdrDatasetVersionEntity v SET v.isCurrent = true WHERE v.id = ?1")
    void setCurrentFlag(Long versionId);

    @Modifying
    @Query("UPDATE RdrDatasetVersionEntity v SET v.accessCount = v.accessCount + 1, " +
           "v.lastAccessedAt = CURRENT_TIMESTAMP WHERE v.id = ?1")
    void incrementAccessCount(Long versionId);

    @Query("SELECT COUNT(v) FROM RdrDatasetVersionEntity v WHERE v.status = 'PUBLISHED'")
    long countPublishedVersions();

    @Query("SELECT v FROM RdrDatasetVersionEntity v WHERE v.status = 'DRAFT' AND v.isDeleted = false")
    List<RdrDatasetVersionEntity> findDraftVersions();

    Optional<RdrDatasetVersionEntity> findByIdAndIsDeletedFalse(Long id);
}