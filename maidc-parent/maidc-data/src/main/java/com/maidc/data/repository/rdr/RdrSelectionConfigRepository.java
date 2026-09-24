package com.maidc.data.repository.rdr;

import com.maidc.data.entity.rdr.RdrSelectionConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RDR队列筛选配置Repository
 */
@Repository
public interface RdrSelectionConfigRepository extends
        JpaRepository<RdrSelectionConfigEntity, Long>,
        JpaSpecificationExecutor<RdrSelectionConfigEntity> {

    Optional<RdrSelectionConfigEntity> findBySelectionCodeAndIsDeletedFalse(String selectionCode);

    List<RdrSelectionConfigEntity> findByCohortIdAndIsDeletedFalse(Long cohortId);

    List<RdrSelectionConfigEntity> findByStatusAndIsDeletedFalse(String status);

    boolean existsBySelectionCodeAndIsDeletedFalse(String selectionCode);

    @Modifying
    @Query("UPDATE RdrSelectionConfigEntity s SET s.executionCount = s.executionCount + 1, " +
           "s.lastExecutionAt = CURRENT_TIMESTAMP WHERE s.id = ?1")
    void incrementExecutionCount(Long selectionId);

    @Query("SELECT COUNT(s) FROM RdrSelectionConfigEntity s WHERE s.status = 'ACTIVE'")
    long countActiveSelections();

    @Query("SELECT s FROM RdrSelectionConfigEntity s WHERE s.templateId = ?1 AND s.isDeleted = false")
    List<RdrSelectionConfigEntity> findByTemplate(Long templateId);

    Optional<RdrSelectionConfigEntity> findByIdAndIsDeletedFalse(Long id);
}