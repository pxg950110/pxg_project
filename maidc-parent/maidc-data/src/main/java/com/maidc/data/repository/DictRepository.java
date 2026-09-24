package com.maidc.data.repository;

import com.maidc.data.entity.DictEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DictRepository extends JpaRepository<DictEntity, Long> {

    List<DictEntity> findByDictTypeAndIsDeletedFalseOrderBySortOrder(String dictType);

    @Query("SELECT DISTINCT d.dictType FROM DictEntity d WHERE d.isDeleted = false ORDER BY d.dictType")
    List<String> findDistinctTypes();

    List<DictEntity> findByIsDeletedFalseOrderByDictTypeAscSortOrderAsc();

    List<DictEntity> findByDictTypeAndParentCodeAndIsDeletedFalseOrderBySortOrder(String dictType, String parentCode);
}
