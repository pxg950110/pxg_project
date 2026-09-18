package com.maidc.data.repository.dictionary;

import com.maidc.data.entity.dictionary.DiagnosisEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosisDictRepository extends DictionaryRepository<DiagnosisEntity> {

    @Query("SELECT d FROM DiagnosisDict d WHERE d.isDeleted = false AND d.icd10Code = :icd10Code")
    Optional<DiagnosisEntity> findByIcd10Code(@Param("icd10Code") String icd10Code);

    @Query("SELECT d FROM DiagnosisDict d WHERE d.isDeleted = false AND d.chapterCode = :chapterCode ORDER BY d.icd10Code")
    List<DiagnosisEntity> findByChapterCode(@Param("chapterCode") String chapterCode);

    @Query("SELECT d FROM DiagnosisDict d WHERE d.isDeleted = false AND d.parentId = :parentId ORDER BY d.diagnosisCode")
    List<DiagnosisEntity> findByParentId(@Param("parentId") Long parentId);

    @Query("SELECT d FROM DiagnosisDict d WHERE d.isDeleted = false AND d.parentId IS NULL ORDER BY d.chapterCode")
    List<DiagnosisEntity> findRootDiagnoses();
}
