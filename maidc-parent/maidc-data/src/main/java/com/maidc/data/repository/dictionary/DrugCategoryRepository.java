package com.maidc.data.repository.dictionary;

import com.maidc.data.entity.dictionary.DrugCategoryEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrugCategoryRepository extends DictionaryRepository<DrugCategoryEntity> {

    boolean existsByCodeAndIsDeletedFalse(String code);

    @Query("SELECT c FROM DrugCategoryEntity c WHERE c.isDeleted = false ORDER BY c.sortOrder")
    List<DrugCategoryEntity> findAllOrderBySortOrder();

    @Query("SELECT c FROM DrugCategoryEntity c WHERE c.isDeleted = false AND c.parentId IS NULL ORDER BY c.sortOrder")
    List<DrugCategoryEntity> findRootCategories();

    @Query("SELECT c FROM DrugCategoryEntity c WHERE c.isDeleted = false AND c.parentId = :parentId ORDER BY c.sortOrder")
    List<DrugCategoryEntity> findByParentId(@Param("parentId") Long parentId);
}
