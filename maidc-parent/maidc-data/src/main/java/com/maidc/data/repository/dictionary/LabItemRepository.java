package com.maidc.data.repository.dictionary;

import com.maidc.data.entity.dictionary.LabItemEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabItemRepository extends DictionaryRepository<LabItemEntity> {

    @Query("SELECT l FROM LabItemEntity l WHERE l.isDeleted = false AND l.loincCode = :loincCode")
    Optional<LabItemEntity> findByLoincCode(@Param("loincCode") String loincCode);

    @Query("SELECT DISTINCT l.labCategory FROM LabItemEntity l WHERE l.isDeleted = false ORDER BY l.labCategory")
    List<String> findAllCategories();

    @Query("SELECT l FROM LabItemEntity l WHERE l.isDeleted = false AND l.parentId = :parentId")
    List<LabItemEntity> findByParentId(@Param("parentId") Long parentId);
}
