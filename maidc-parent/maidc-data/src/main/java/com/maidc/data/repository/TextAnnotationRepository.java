package com.maidc.data.repository;

import com.maidc.data.entity.TextAnnotationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TextAnnotationRepository extends JpaRepository<TextAnnotationEntity, Long>,
        JpaSpecificationExecutor<TextAnnotationEntity> {
}
