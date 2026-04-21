package com.maidc.data.repository;

import com.maidc.data.entity.DiseaseTemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiseaseTemplateRepository extends JpaRepository<DiseaseTemplateEntity, Long> {

    List<DiseaseTemplateEntity> findByDiseaseNameContainingAndIsDeletedFalse(String keyword);

    Page<DiseaseTemplateEntity> findByIsDeletedFalse(Pageable pageable);
}
