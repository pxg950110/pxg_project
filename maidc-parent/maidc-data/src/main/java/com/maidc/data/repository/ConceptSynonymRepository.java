package com.maidc.data.repository;

import com.maidc.data.entity.ConceptSynonymEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConceptSynonymRepository extends JpaRepository<ConceptSynonymEntity, Long> {

    List<ConceptSynonymEntity> findByConceptIdAndIsDeletedFalse(Long conceptId);
}
