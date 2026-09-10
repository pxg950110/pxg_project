package com.maidc.data.repository;

import com.maidc.data.entity.ScaleDefinitionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScaleDefinitionRepository extends JpaRepository<ScaleDefinitionEntity, Long> {

    Optional<ScaleDefinitionEntity> findByIdAndIsDeletedFalse(Long id);

    Optional<ScaleDefinitionEntity> findByScaleCodeAndVersionAndIsDeletedFalse(String scaleCode, Integer version);

    Optional<ScaleDefinitionEntity> findFirstByScaleCodeAndStatusAndIsDeletedFalseOrderByVersionDesc(String scaleCode, String status);

    List<ScaleDefinitionEntity> findByScaleCodeAndIsDeletedFalseOrderByVersionDesc(String scaleCode);

    Page<ScaleDefinitionEntity> findByIsDeletedFalse(Pageable pageable);

    @Query("SELECT s FROM com.maidc.data.entity.ScaleDefinitionEntity s WHERE s.isDeleted = false " +
           "AND (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.scaleCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ScaleDefinitionEntity> search(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT COUNT(DISTINCT p.protocol_id) FROM cdr.c_followup_protocol p " +
           "WHERE p.is_deleted = false AND p.stages::text LIKE CONCAT('%\"', :scaleCode, '\"%')",
           nativeQuery = true)
    long countProtocolsReferencing(@Param("scaleCode") String scaleCode);
}
