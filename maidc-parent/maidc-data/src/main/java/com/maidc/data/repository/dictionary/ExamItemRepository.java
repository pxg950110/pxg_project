package com.maidc.data.repository.dictionary;

import com.maidc.data.entity.dictionary.ExamItemEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamItemRepository extends DictionaryRepository<ExamItemEntity> {

    @Query("SELECT DISTINCT e.examType FROM ExamItemEntity e WHERE e.isDeleted = false ORDER BY e.examType")
    List<String> findAllExamTypes();
}
