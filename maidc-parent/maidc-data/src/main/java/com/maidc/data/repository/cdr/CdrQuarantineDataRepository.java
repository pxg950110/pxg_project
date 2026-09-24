package com.maidc.data.repository.cdr;

import com.maidc.data.entity.cdr.CdrQuarantineDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CDR数据隔离区Repository
 */
@Repository
public interface CdrQuarantineDataRepository extends
        JpaRepository<CdrQuarantineDataEntity, Long>,
        JpaSpecificationExecutor<CdrQuarantineDataEntity> {

    Optional<CdrQuarantineDataEntity> findByQuarantineCodeAndIsDeletedFalse(String quarantineCode);

    List<CdrQuarantineDataEntity> findByStatusAndIsDeletedFalse(String status);

    List<CdrQuarantineDataEntity> findBySourceSchemaAndSourceTableAndIsDeletedFalse(String schema, String table);

    List<CdrQuarantineDataEntity> findByAssignedToAndStatusAndIsDeletedFalse(String assignedTo, String status);

    List<CdrQuarantineDataEntity> findByPriorityAndStatusAndIsDeletedFalse(String priority, String status);

    boolean existsByQuarantineCodeAndIsDeletedFalse(String quarantineCode);

    @Modifying
    @Query("UPDATE CdrQuarantineDataEntity q SET q.status = ?2, q.assignedTo = ?3, " +
           "q.assignedAt = CURRENT_TIMESTAMP WHERE q.id = ?1")
    void assignQuarantine(Long id, String status, String assignedTo);

    @Modifying
    @Query("UPDATE CdrQuarantineDataEntity q SET q.status = ?2, q.processedBy = ?3, " +
           "q.processedAt = CURRENT_TIMESTAMP, q.processAction = ?4, q.fixedData = ?5, " +
           "q.processNotes = ?6 WHERE q.id = ?1")
    void processQuarantine(Long id, String status, String processedBy, String action,
                           String fixedData, String notes);

    @Query("SELECT COUNT(q) FROM CdrQuarantineDataEntity q WHERE q.status = 'PENDING'")
    long countPendingQuarantines();

    @Query("SELECT COUNT(q) FROM CdrQuarantineDataEntity q WHERE q.status = 'PROCESSING'")
    long countProcessingQuarantines();

    @Query("SELECT COUNT(q) FROM CdrQuarantineDataEntity q WHERE q.status = 'FIXED'")
    long countFixedQuarantines();

    Optional<CdrQuarantineDataEntity> findByIdAndIsDeletedFalse(Long id);
}