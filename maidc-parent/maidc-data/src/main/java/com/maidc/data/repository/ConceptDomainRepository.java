package com.maidc.data.repository;

import com.maidc.data.entity.ConceptDomainEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConceptDomainRepository extends JpaRepository<ConceptDomainEntity, Long>, JpaSpecificationExecutor<ConceptDomainEntity> {

    Optional<ConceptDomainEntity> findByIdAndIsDeletedFalse(Long id);

    Optional<ConceptDomainEntity> findByCodeAndIsDeletedFalse(String code);

    Page<ConceptDomainEntity> findByIsDeletedFalse(Pageable pageable);

    Page<ConceptDomainEntity> findByDomainTypeAndIsDeletedFalse(String domainType, Pageable pageable);

    List<ConceptDomainEntity> findByParentIdAndIsDeletedFalse(Long parentId);

    List<ConceptDomainEntity> findByIsDeletedFalse();

    @Query("SELECT cd FROM ConceptDomainEntity cd WHERE cd.isDeleted = false AND (LOWER(cd.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR cd.code LIKE CONCAT('%', :keyword, '%') OR LOWER(cd.definition) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ConceptDomainEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT cd FROM ConceptDomainEntity cd WHERE cd.isDeleted = false AND cd.domainType = :domainType AND (LOWER(cd.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR cd.code LIKE CONCAT('%', :keyword, '%'))")
    Page<ConceptDomainEntity> searchByKeywordAndType(@Param("keyword") String keyword, @Param("domainType") String domainType, Pageable pageable);

    boolean existsByCodeAndIsDeletedFalse(String code);
}
