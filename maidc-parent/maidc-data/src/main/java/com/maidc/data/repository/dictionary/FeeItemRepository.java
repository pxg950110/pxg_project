package com.maidc.data.repository.dictionary;

import com.maidc.data.entity.dictionary.FeeItemEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeItemRepository extends DictionaryRepository<FeeItemEntity> {

    @Query("SELECT DISTINCT f.feeCategory FROM FeeItemEntity f WHERE f.isDeleted = false ORDER BY f.feeCategory")
    List<String> findAllCategories();
}
