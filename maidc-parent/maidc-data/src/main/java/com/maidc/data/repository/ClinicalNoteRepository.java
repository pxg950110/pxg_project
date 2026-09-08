package com.maidc.data.repository;

import com.maidc.data.entity.ClinicalNoteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClinicalNoteRepository extends JpaRepository<ClinicalNoteEntity, Long>, JpaSpecificationExecutor<ClinicalNoteEntity> {

    Page<ClinicalNoteEntity> findByEncounterId(Long encounterId, Pageable pageable);

    Page<ClinicalNoteEntity> findByNoteType(String noteType, Pageable pageable);

    Page<ClinicalNoteEntity> findByNoteCategory(String noteCategory, Pageable pageable);

    Page<ClinicalNoteEntity> findByEncounterIdAndNoteType(Long encounterId, String noteType, Pageable pageable);

    List<ClinicalNoteEntity> findByParentId(Long parentId);
}
