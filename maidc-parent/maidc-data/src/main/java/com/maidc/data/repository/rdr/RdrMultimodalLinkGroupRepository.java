package com.maidc.data.repository.rdr;

import com.maidc.data.entity.rdr.RdrMultimodalLinkGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RDR多模态关联组Repository
 */
@Repository
public interface RdrMultimodalLinkGroupRepository extends
        JpaRepository<RdrMultimodalLinkGroupEntity, Long>,
        JpaSpecificationExecutor<RdrMultimodalLinkGroupEntity> {

    Optional<RdrMultimodalLinkGroupEntity> findByGroupCodeAndIsDeletedFalse(String groupCode);

    List<RdrMultimodalLinkGroupEntity> findByPatientIdAndIsDeletedFalse(Long patientId);

    List<RdrMultimodalLinkGroupEntity> findByEncounterIdAndIsDeletedFalse(Long encounterId);

    List<RdrMultimodalLinkGroupEntity> findByStatusAndIsDeletedFalse(String status);

    boolean existsByGroupCodeAndIsDeletedFalse(String groupCode);

    @Query("SELECT g FROM RdrMultimodalLinkGroupEntity g WHERE g.patientId = ?1 " +
           "AND g.status = 'ACTIVE' AND g.isDeleted = false")
    List<RdrMultimodalLinkGroupEntity> findActiveGroupsForPatient(Long patientId);

    @Query("SELECT AVG(g.completenessScore) FROM RdrMultimodalLinkGroupEntity g WHERE g.status = 'ACTIVE'")
    Double getAverageCompletenessScore();

    @Query("SELECT g.groupType, COUNT(g) FROM RdrMultimodalLinkGroupEntity g " +
           "WHERE g.isDeleted = false GROUP BY g.groupType")
    List<Object[]> countByGroupType();

    Optional<RdrMultimodalLinkGroupEntity> findByIdAndIsDeletedFalse(Long id);
}