package com.maidc.data.repository;

import com.maidc.data.entity.PropertyEntity;
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
public interface PropertyRepository extends JpaRepository<PropertyEntity, Long>, JpaSpecificationExecutor<PropertyEntity> {

    Optional<PropertyEntity> findByIdAndIsDeletedFalse(Long id);

    Optional<PropertyEntity> findByCodeAndIsDeletedFalse(String code);

    Page<PropertyEntity> findByIsDeletedFalse(Pageable pageable);

    List<PropertyEntity> findByParentIdAndIsDeletedFalse(Long parentId);

    List<PropertyEntity> findByIsDeletedFalse();

    @Query("SELECT p FROM PropertyEntity p WHERE p.isDeleted = false AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR p.code LIKE CONCAT('%', :keyword, '%'))")
    Page<PropertyEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    boolean existsByCodeAndIsDeletedFalse(String code);
}
