package com.maidc.data.repository;

import com.maidc.data.entity.PermissibleValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissibleValueRepository extends JpaRepository<PermissibleValueEntity, Long>, JpaSpecificationExecutor<PermissibleValueEntity> {

    Optional<PermissibleValueEntity> findByIdAndIsDeletedFalse(Long id);

    List<PermissibleValueEntity> findByValueDomainIdAndIsDeletedFalseOrderBySortOrder(Long valueDomainId);

    Optional<PermissibleValueEntity> findByValueDomainIdAndValueAndIsDeletedFalse(Long valueDomainId, String value);

    @Query("SELECT pv FROM PermissibleValueEntity pv WHERE pv.isDeleted = false AND pv.valueDomainId = :valueDomainId AND LOWER(pv.value) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<PermissibleValueEntity> searchByKeyword(@Param("valueDomainId") Long valueDomainId, @Param("keyword") String keyword);

    boolean existsByValueDomainIdAndValueAndIsDeletedFalse(Long valueDomainId, String value);

    long countByValueDomainIdAndIsDeletedFalse(Long valueDomainId);
}
