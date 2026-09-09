package com.maidc.data.repository;

import com.maidc.data.entity.DiseaseKbItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiseaseKbItemRepository extends JpaRepository<DiseaseKbItemEntity, Long>, JpaSpecificationExecutor<DiseaseKbItemEntity> {

    Page<DiseaseKbItemEntity> findBySpaceId(Long spaceId, Pageable pageable);

    long countBySpaceIdAndIsDeletedFalse(Long spaceId);

    List<DiseaseKbItemEntity> findBySpaceIdAndStatusAndIsDeletedFalse(Long spaceId, String status);

    @Query("SELECT i.itemType AS type, COUNT(i) AS cnt FROM DiseaseKbItemEntity i " +
           "WHERE i.spaceId = :spaceId AND i.isDeleted = false AND i.status <> 'ARCHIVED' " +
           "GROUP BY i.itemType")
    List<TypeCount> countBySpaceGroupByType(@Param("spaceId") Long spaceId);

    interface TypeCount {
        String getType();
        long getCnt();
    }
}
