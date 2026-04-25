package com.maidc.data.repository;

import com.maidc.data.entity.LocalConceptEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocalConceptRepository extends JpaRepository<LocalConceptEntity, Long>, JpaSpecificationExecutor<LocalConceptEntity> {

    Optional<LocalConceptEntity> findByInstitutionIdAndCodeSystemIdAndLocalCodeAndIsDeletedFalse(Long institutionId, Long codeSystemId, String localCode);

    Page<LocalConceptEntity> findByInstitutionIdAndCodeSystemIdAndIsDeletedFalse(Long institutionId, Long codeSystemId, Pageable pageable);

    Page<LocalConceptEntity> findByInstitutionIdAndCodeSystemIdAndMappingStatusAndIsDeletedFalse(Long institutionId, Long codeSystemId, String mappingStatus, Pageable pageable);

    @Query("SELECT lc FROM LocalConceptEntity lc WHERE lc.isDeleted = false AND lc.mappingStatus = 'UNMAPPED' " +
            "AND (:institutionId IS NULL OR lc.institutionId = :institutionId) " +
            "AND (:codeSystemId IS NULL OR lc.codeSystemId = :codeSystemId)")
    Page<LocalConceptEntity> findUnmapped(@Param("institutionId") Long institutionId,
                                          @Param("codeSystemId") Long codeSystemId,
                                          Pageable pageable);

    @Query("SELECT COUNT(lc) FROM LocalConceptEntity lc WHERE lc.isDeleted = false " +
            "AND (:institutionId IS NULL OR lc.institutionId = :institutionId) " +
            "AND (:codeSystemId IS NULL OR lc.codeSystemId = :codeSystemId) " +
            "AND lc.mappingStatus = :status")
    long countByStatus(@Param("institutionId") Long institutionId,
                       @Param("codeSystemId") Long codeSystemId,
                       @Param("status") String status);
}
