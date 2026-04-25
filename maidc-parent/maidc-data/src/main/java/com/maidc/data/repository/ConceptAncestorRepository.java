package com.maidc.data.repository;

import com.maidc.data.entity.ConceptAncestorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConceptAncestorRepository extends JpaRepository<ConceptAncestorEntity, Long> {

    List<ConceptAncestorEntity> findByDescendantConceptId(Long descendantId);

    List<ConceptAncestorEntity> findByAncestorConceptId(Long ancestorId);

    List<ConceptAncestorEntity> findByAncestorConceptIdAndMinLevelsOfSeparation(Long ancestorId, int levels);
}
