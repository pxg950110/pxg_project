package com.maidc.data.repository.cdr;

import com.maidc.data.entity.cdr.CdrDataLineageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CDR数据血缘Repository
 */
@Repository
public interface CdrDataLineageRepository extends
        JpaRepository<CdrDataLineageEntity, Long>,
        JpaSpecificationExecutor<CdrDataLineageEntity> {

    Optional<CdrDataLineageEntity> findByLineageCodeAndIsDeletedFalse(String lineageCode);

    List<CdrDataLineageEntity> findByTargetSchemaAndTargetTableAndIsDeletedFalse(String schema, String table);

    List<CdrDataLineageEntity> findByTargetSchemaAndTargetTableAndTargetColumnAndIsDeletedFalse(
            String schema, String table, String column);

    List<CdrDataLineageEntity> findBySourceSystemAndSourceTableAndIsDeletedFalse(String system, String table);

    List<CdrDataLineageEntity> findByEtlJobIdAndIsDeletedFalse(Long etlJobId);

    boolean existsByLineageCodeAndIsDeletedFalse(String lineageCode);

    @Modifying
    @Query("UPDATE CdrDataLineageEntity l SET l.syncCount = l.syncCount + 1, " +
           "l.lastSyncTime = CURRENT_TIMESTAMP WHERE l.id = ?1")
    void incrementSyncCount(Long lineageId);

    @Query("SELECT l.sourceSystem, COUNT(l) FROM CdrDataLineageEntity l " +
           "WHERE l.isDeleted = false GROUP BY l.sourceSystem")
    List<Object[]> countBySourceSystem();

    @Query("SELECT l FROM CdrDataLineageEntity l WHERE l.targetSchema = ?1 " +
           "AND l.targetTable = ?2 AND l.targetColumn = ?3 AND l.isDeleted = false")
    List<CdrDataLineageEntity> findLineageForColumn(String schema, String table, String column);

    @Query("SELECT DISTINCT l.sourceSystem FROM CdrDataLineageEntity l WHERE l.isDeleted = false")
    List<String> findDistinctSourceSystems();

    Optional<CdrDataLineageEntity> findByIdAndIsDeletedFalse(Long id);
}