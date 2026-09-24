package com.maidc.data.repository;

import com.maidc.data.entity.ObjectClassEntity;
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
public interface ObjectClassRepository extends JpaRepository<ObjectClassEntity, Long>, JpaSpecificationExecutor<ObjectClassEntity> {

    Optional<ObjectClassEntity> findByIdAndIsDeletedFalse(Long id);

    Optional<ObjectClassEntity> findByCodeAndIsDeletedFalse(String code);

    Page<ObjectClassEntity> findByIsDeletedFalse(Pageable pageable);

    List<ObjectClassEntity> findByParentIdAndIsDeletedFalse(Long parentId);

    List<ObjectClassEntity> findByIsDeletedFalse();

    @Query("SELECT oc FROM ObjectClassEntity oc WHERE oc.isDeleted = false AND (LOWER(oc.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR oc.code LIKE CONCAT('%', :keyword, '%'))")
    Page<ObjectClassEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    boolean existsByCodeAndIsDeletedFalse(String code);
}
