package com.maidc.data.repository;

import com.maidc.data.entity.ClinicalNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ClinicalNoteRepository extends JpaRepository<ClinicalNoteEntity, Long>, JpaSpecificationExecutor<ClinicalNoteEntity> {
}
