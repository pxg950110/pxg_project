package com.maidc.data.repository;

import com.maidc.data.entity.DataElementConceptEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DataElementConceptRepository extends JpaRepository<DataElementConceptEntity, Long>, JpaSpecificationExecutor<DataElementConceptEntity> {

    Optional<DataElementConceptEntity> findByIdAndIsDeletedFalse(Long id);

    Optional<DataElementConceptEntity> findByCodeAndIsDeletedFalse(String code);

    Page<DataElementConceptEntity> findByIsDeletedFalse(Pageable pageable);

    Page<DataElementConceptEntity> findByConceptDomainIdAndIsDeletedFalse(Long conceptDomainId, Pageable pageable);

    @Query("SELECT dec FROM DataElementConceptEntity dec WHERE dec.isDeleted = false AND (LOWER(dec.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR dec.code LIKE CONCAT('%', :keyword, '%'))")
    Page<DataElementConceptEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    boolean existsByCodeAndIsDeletedFalse(String code);
}
