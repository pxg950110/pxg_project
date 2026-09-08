package com.maidc.data.repository;

import com.maidc.data.entity.ConceptRelationshipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConceptRelationshipRepository extends JpaRepository<ConceptRelationshipEntity, Long>, JpaSpecificationExecutor<ConceptRelationshipEntity> {

    List<ConceptRelationshipEntity> findByConceptId1AndRelationshipTypeAndIsDeletedFalse(Long conceptId, String type);

    List<ConceptRelationshipEntity> findByConceptId2AndRelationshipTypeAndIsDeletedFalse(Long conceptId, String type);
}
