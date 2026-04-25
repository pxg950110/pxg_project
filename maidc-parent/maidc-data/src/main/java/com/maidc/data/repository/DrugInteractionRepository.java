package com.maidc.data.repository;

import com.maidc.data.entity.DrugInteractionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrugInteractionRepository extends JpaRepository<DrugInteractionEntity, Long> {

    /**
     * Bidirectional lookup: finds interaction between two drugs regardless of order.
     */
    @Query("SELECT d FROM DrugInteractionEntity d WHERE d.isDeleted = false " +
            "AND ((d.drugConceptId1 = :drug1 AND d.drugConceptId2 = :drug2) " +
            "OR (d.drugConceptId1 = :drug2 AND d.drugConceptId2 = :drug1))")
    List<DrugInteractionEntity> findBetween(@Param("drug1") Long drug1, @Param("drug2") Long drug2);

    /**
     * Finds all interactions where both drug IDs are in the given list,
     * ordered by severity (CONTRAINDICATED > MAJOR > MODERATE > MINOR).
     */
    @Query("SELECT d FROM DrugInteractionEntity d WHERE d.isDeleted = false " +
            "AND d.drugConceptId1 IN :ids AND d.drugConceptId2 IN :ids " +
            "ORDER BY CASE d.severity " +
            "  WHEN 'CONTRAINDICATED' THEN 0 " +
            "  WHEN 'MAJOR' THEN 1 " +
            "  WHEN 'MODERATE' THEN 2 " +
            "  WHEN 'MINOR' THEN 3 " +
            "  ELSE 4 END")
    List<DrugInteractionEntity> findInPairSet(@Param("ids") List<Long> ids);

    List<DrugInteractionEntity> findAllByIsDeletedFalse();
}
