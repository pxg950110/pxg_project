package com.maidc.data.repository;

import com.maidc.data.entity.ValueMeaningEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValueMeaningRepository extends JpaRepository<ValueMeaningEntity, Long>, JpaSpecificationExecutor<ValueMeaningEntity> {

    Optional<ValueMeaningEntity> findByIdAndIsDeletedFalse(Long id);

    List<ValueMeaningEntity> findByConceptDomainIdAndIsDeletedFalseOrderBySortOrder(Long conceptDomainId);

    Optional<ValueMeaningEntity> findByConceptDomainIdAndCodeAndIsDeletedFalse(Long conceptDomainId, String code);

    @Query("SELECT vm FROM ValueMeaningEntity vm WHERE vm.isDeleted = false AND vm.conceptDomainId = :conceptDomainId AND (LOWER(vm.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR vm.code LIKE CONCAT('%', :keyword, '%'))")
    List<ValueMeaningEntity> searchByKeyword(@Param("conceptDomainId") Long conceptDomainId, @Param("keyword") String keyword);

    boolean existsByConceptDomainIdAndCodeAndIsDeletedFalse(Long conceptDomainId, String code);

    long countByConceptDomainIdAndIsDeletedFalse(Long conceptDomainId);
}
