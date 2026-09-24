package com.maidc.data.repository;

import com.maidc.data.entity.DocumentTemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentTemplateRepository
        extends JpaRepository<DocumentTemplateEntity, Long>, JpaSpecificationExecutor<DocumentTemplateEntity> {

    Optional<DocumentTemplateEntity> findByCodeAndIsDeletedFalse(String code);

    Page<DocumentTemplateEntity> findByNoteTypeAndIsDeletedFalse(String noteType, Pageable pageable);

    boolean existsByCodeAndIsDeletedFalse(String code);
}
