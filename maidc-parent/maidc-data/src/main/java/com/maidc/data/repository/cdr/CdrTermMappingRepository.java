package com.maidc.data.repository.cdr;

import com.maidc.data.entity.cdr.CdrTermMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CDR术语映射Repository
 */
@Repository
public interface CdrTermMappingRepository extends
        JpaRepository<CdrTermMappingEntity, Long>,
        JpaSpecificationExecutor<CdrTermMappingEntity> {

    Optional<CdrTermMappingEntity> findBySourceSystemAndTermTypeAndLocalCodeAndIsDeletedFalse(
            String sourceSystem, String termType, String localCode);

    List<CdrTermMappingEntity> findByTermTypeAndIsDeletedFalse(String termType);

    List<CdrTermMappingEntity> findByStandardSystemAndIsDeletedFalse(String standardSystem);

    List<CdrTermMappingEntity> findByMappingStatusAndIsDeletedFalse(String mappingStatus);

    List<CdrTermMappingEntity> findByLocalCodeContainingOrLocalNameContainingAndIsDeletedFalse(
            String localCode, String localName);

    @Query("SELECT t FROM CdrTermMappingEntity t WHERE t.localName LIKE %?1% " +
           "AND t.standardSystem = ?2 AND t.isDeleted = false ORDER BY t.confidence DESC")
    List<CdrTermMappingEntity> searchByLocalNameAndStandardSystem(String localName, String standardSystem);

    @Modifying
    @Query("UPDATE CdrTermMappingEntity t SET t.mappingStatus = ?2, t.validatedBy = ?3, " +
           "t.validatedAt = CURRENT_TIMESTAMP WHERE t.id = ?1")
    void updateMappingStatus(Long id, String status, String validatedBy);

    @Modifying
    @Query("UPDATE CdrTermMappingEntity t SET t.useCount = t.useCount + 1, " +
           "t.lastUsedAt = CURRENT_TIMESTAMP WHERE t.id = ?1")
    void incrementUseCount(Long mappingId);

    @Query("SELECT COUNT(t) FROM CdrTermMappingEntity t WHERE t.mappingStatus = 'PENDING'")
    long countPendingMappings();

    @Query("SELECT COUNT(t) FROM CdrTermMappingEntity t WHERE t.mappingStatus = 'CONFIRMED'")
    long countConfirmedMappings();

    @Query("SELECT t.standardSystem, COUNT(t) FROM CdrTermMappingEntity t " +
           "WHERE t.mappingStatus = 'CONFIRMED' AND t.isDeleted = false GROUP BY t.standardSystem")
    List<Object[]> countByStandardSystem();

    Optional<CdrTermMappingEntity> findByIdAndIsDeletedFalse(Long id);
}