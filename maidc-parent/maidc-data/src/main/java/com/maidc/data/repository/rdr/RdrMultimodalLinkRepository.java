package com.maidc.data.repository.rdr;

import com.maidc.data.entity.rdr.RdrMultimodalLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RDR多模态数据关联Repository
 */
@Repository
public interface RdrMultimodalLinkRepository extends
        JpaRepository<RdrMultimodalLinkEntity, Long>,
        JpaSpecificationExecutor<RdrMultimodalLinkEntity> {

    List<RdrMultimodalLinkEntity> findByPatientIdAndIsDeletedFalse(Long patientId);

    List<RdrMultimodalLinkEntity> findByEncounterIdAndIsDeletedFalse(Long encounterId);

    List<RdrMultimodalLinkEntity> findByLinkGroupIdAndIsDeletedFalse(Long linkGroupId);

    List<RdrMultimodalLinkEntity> findByModalityTypeAndIsDeletedFalse(String modalityType);

    List<RdrMultimodalLinkEntity> findByPatientIdAndModalityTypeAndIsDeletedFalse(Long patientId, String modalityType);

    @Modifying
    @Query("UPDATE RdrMultimodalLinkEntity l SET l.isValidated = true, l.validatedBy = ?2, " +
           "l.validatedAt = CURRENT_TIMESTAMP WHERE l.id = ?1")
    void validateLink(Long linkId, String validatedBy);

    @Query("SELECT l.modalityType, COUNT(l) FROM RdrMultimodalLinkEntity l " +
           "WHERE l.patientId = ?1 AND l.isDeleted = false GROUP BY l.modalityType")
    List<Object[]> countByModalityTypeForPatient(Long patientId);

    @Query("SELECT l FROM RdrMultimodalLinkEntity l WHERE l.patientId = ?1 " +
           "AND l.timestamp >= ?2 AND l.timestamp <= ?3 AND l.isDeleted = false")
    List<RdrMultimodalLinkEntity> findByPatientAndTimeRange(Long patientId,
            java.time.LocalDateTime start, java.time.LocalDateTime end);

    @Query("SELECT COUNT(l) FROM RdrMultimodalLinkEntity l WHERE l.isValidated = false")
    long countUnvalidatedLinks();

    @Query("SELECT AVG(l.qualityScore) FROM RdrMultimodalLinkEntity l WHERE l.status = 'ACTIVE'")
    Double getAverageQualityScore();

    Optional<RdrMultimodalLinkEntity> findByIdAndIsDeletedFalse(Long id);
}