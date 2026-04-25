package com.maidc.data.repository;

import com.maidc.data.entity.ReferenceRangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferenceRangeRepository extends JpaRepository<ReferenceRangeEntity, Long> {

    Optional<ReferenceRangeEntity> findByIdAndIsDeletedFalse(Long id);

    List<ReferenceRangeEntity> findByConceptIdAndIsDeletedFalse(Long conceptId);

    List<ReferenceRangeEntity> findAllByIsDeletedFalse();

    @Query("SELECT r FROM ReferenceRangeEntity r WHERE r.isDeleted = false " +
            "AND r.conceptId = :conceptId " +
            "AND (r.gender = :gender OR r.gender = 'ALL') " +
            "AND (r.ageMin IS NULL OR r.ageMin <= :age) " +
            "AND (r.ageMax IS NULL OR r.ageMax >= :age) " +
            "ORDER BY CASE " +
            "  WHEN r.gender = :gender AND r.ageMin IS NOT NULL THEN 0 " +
            "  WHEN r.gender = :gender THEN 1 " +
            "  ELSE 2 END")
    List<ReferenceRangeEntity> findBestMatch(@Param("conceptId") Long conceptId,
                                             @Param("gender") String gender,
                                             @Param("age") BigDecimal age);
}
