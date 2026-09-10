package com.maidc.data.repository;

import com.maidc.data.entity.TreatmentRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TreatmentRecordRepository extends JpaRepository<TreatmentRecordEntity, Long> {

    List<TreatmentRecordEntity> findByFollowupIdAndIsDeletedFalseOrderByOccurredDateDesc(Long followupId);

    List<TreatmentRecordEntity> findByFollowupIdInAndIsDeletedFalse(List<Long> followupIds);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM com.maidc.data.entity.TreatmentRecordEntity t " +
           "WHERE t.followupId = :followupId AND t.name = :name AND t.occurredDate = :occurredDate " +
           "AND t.source = 'CDR' AND t.isDeleted = false")
    boolean existsCdrDuplicate(@Param("followupId") Long followupId, @Param("name") String name, @Param("occurredDate") LocalDate occurredDate);
}
