package com.maidc.data.repository;

import com.maidc.data.entity.DataElementEntity;
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
public interface DataElementRepository extends JpaRepository<DataElementEntity, Long>, JpaSpecificationExecutor<DataElementEntity> {

    Optional<DataElementEntity> findByIdAndIsDeletedFalse(Long id);

    boolean existsByElementCodeAndIsDeletedFalse(String elementCode);

    @Query("SELECT DISTINCT e.category FROM DataElementEntity e WHERE e.isDeleted = false AND e.category IS NOT NULL ORDER BY e.category")
    List<String> findDistinctCategories();

    @Query("SELECT e FROM DataElementEntity e WHERE e.isDeleted = false AND " +
           "(LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "e.elementCode LIKE CONCAT('%', :keyword, '%') OR " +
           "LOWER(e.definition) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<DataElementEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
