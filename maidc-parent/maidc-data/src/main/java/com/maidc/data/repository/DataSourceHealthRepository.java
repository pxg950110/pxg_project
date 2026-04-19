package com.maidc.data.repository;

import com.maidc.data.entity.DataSourceHealthEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DataSourceHealthRepository extends JpaRepository<DataSourceHealthEntity, Long> {

    List<DataSourceHealthEntity> findTop50BySourceIdAndIsDeletedFalseOrderByCheckedAtDesc(Long sourceId);

    Page<DataSourceHealthEntity> findBySourceIdAndIsDeletedFalseOrderByCheckedAtDesc(Long sourceId, Pageable pageable);

    @Query("SELECT h FROM DataSourceHealthEntity h WHERE h.sourceId = :sourceId " +
           "AND h.isDeleted = false AND h.checkedAt >= :since ORDER BY h.checkedAt DESC")
    List<DataSourceHealthEntity> findRecentHealth(@Param("sourceId") Long sourceId,
                                                   @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(h) FROM DataSourceHealthEntity h WHERE h.sourceId = :sourceId " +
           "AND h.isDeleted = false AND h.checkedAt >= :since")
    long countSince(@Param("sourceId") Long sourceId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(h) FROM DataSourceHealthEntity h WHERE h.sourceId = :sourceId " +
           "AND h.isDeleted = false AND h.status = 'SUCCESS' AND h.checkedAt >= :since")
    long countSuccessSince(@Param("sourceId") Long sourceId, @Param("since") LocalDateTime since);

    @Query("SELECT AVG(CAST(h.latencyMs AS double)) FROM DataSourceHealthEntity h " +
           "WHERE h.sourceId = :sourceId AND h.isDeleted = false AND h.status = 'SUCCESS' AND h.checkedAt >= :since")
    Double avgLatencySince(@Param("sourceId") Long sourceId, @Param("since") LocalDateTime since);
}