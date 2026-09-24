package com.maidc.data.repository;

import com.maidc.data.entity.DictTypeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DictTypeRepository extends JpaRepository<DictTypeEntity, Long>, JpaSpecificationExecutor<DictTypeEntity> {

    Optional<DictTypeEntity> findByIdAndIsDeletedFalse(Long id);

    Optional<DictTypeEntity> findByTypeCodeAndIsDeletedFalse(String typeCode);

    boolean existsByTypeCodeAndIsDeletedFalse(String typeCode);

    @Query("SELECT t FROM DictTypeEntity t WHERE t.isDeleted = false AND " +
           "(LOWER(t.typeName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.typeCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<DictTypeEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Page<DictTypeEntity> findByIsDeletedFalse(Pageable pageable);
}
