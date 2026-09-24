package com.maidc.data.repository;

import com.maidc.data.entity.ValueDomainEntity;
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
public interface ValueDomainRepository extends JpaRepository<ValueDomainEntity, Long>, JpaSpecificationExecutor<ValueDomainEntity> {

    Optional<ValueDomainEntity> findByIdAndIsDeletedFalse(Long id);

    Optional<ValueDomainEntity> findByCodeAndIsDeletedFalse(String code);

    Page<ValueDomainEntity> findByIsDeletedFalse(Pageable pageable);

    Page<ValueDomainEntity> findByConceptDomainIdAndIsDeletedFalse(Long conceptDomainId, Pageable pageable);

    Page<ValueDomainEntity> findByDomainTypeAndIsDeletedFalse(String domainType, Pageable pageable);

    List<ValueDomainEntity> findByIsDeletedFalse();

    @Query("SELECT vd FROM ValueDomainEntity vd WHERE vd.isDeleted = false AND (LOWER(vd.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR vd.code LIKE CONCAT('%', :keyword, '%'))")
    Page<ValueDomainEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT vd FROM ValueDomainEntity vd WHERE vd.isDeleted = false AND vd.conceptDomainId = :conceptDomainId AND (LOWER(vd.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR vd.code LIKE CONCAT('%', :keyword, '%'))")
    Page<ValueDomainEntity> searchByKeywordAndConceptDomain(@Param("keyword") String keyword, @Param("conceptDomainId") Long conceptDomainId, Pageable pageable);

    boolean existsByCodeAndIsDeletedFalse(String code);
}
