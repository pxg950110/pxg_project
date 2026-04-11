package com.maidc.data.repository;

import com.maidc.data.entity.ImagingAnnotationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagingAnnotationRepository extends JpaRepository<ImagingAnnotationEntity, Long>,
        JpaSpecificationExecutor<ImagingAnnotationEntity> {
}
