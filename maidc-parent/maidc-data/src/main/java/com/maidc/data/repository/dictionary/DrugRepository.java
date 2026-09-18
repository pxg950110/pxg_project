package com.maidc.data.repository.dictionary;

import com.maidc.data.entity.dictionary.DrugEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrugRepository extends DictionaryRepository<DrugEntity> {

    Optional<DrugEntity> findByDrugCodeAndIsDeletedFalse(String drugCode);

    @Query("SELECT d FROM DrugEntity d WHERE d.isDeleted = false AND d.atcCode = :atcCode")
    List<DrugEntity> findByAtcCode(@Param("atcCode") String atcCode);

    @Query("SELECT COUNT(d) FROM DrugEntity d WHERE d.isDeleted = false AND d.categoryId = :categoryId")
    long countByCategory(@Param("categoryId") Long categoryId);
}
