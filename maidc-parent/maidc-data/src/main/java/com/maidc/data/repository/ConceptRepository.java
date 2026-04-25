package com.maidc.data.repository;

import com.maidc.data.entity.ConceptEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConceptRepository extends JpaRepository<ConceptEntity, Long>, JpaSpecificationExecutor<ConceptEntity> {

    Optional<ConceptEntity> findByIdAndIsDeletedFalse(Long id);

    Page<ConceptEntity> findByCodeSystemIdAndIsDeletedFalse(Long codeSystemId, Pageable pageable);

    @Query("SELECT c FROM ConceptEntity c WHERE c.isDeleted = false AND c.status = 'ACTIVE' AND c.codeSystemId = :systemId AND c.conceptCode = :code")
    Optional<ConceptEntity> findBySystemAndCode(@Param("systemId") Long systemId, @Param("code") String code);

    @Query("SELECT c FROM ConceptEntity c WHERE c.isDeleted = false AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR c.conceptCode LIKE CONCAT('%', :keyword, '%'))")
    Page<ConceptEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM ConceptEntity c WHERE c.isDeleted = false AND c.codeSystemId = :systemId AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR c.conceptCode LIKE CONCAT('%', :keyword, '%'))")
    Page<ConceptEntity> searchByKeywordAndSystem(@Param("keyword") String keyword, @Param("systemId") Long systemId, Pageable pageable);
}
