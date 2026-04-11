package com.maidc.data.repository;

import com.maidc.data.entity.ImagingExamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagingExamRepository extends JpaRepository<ImagingExamEntity, Long>, JpaSpecificationExecutor<ImagingExamEntity> {
}
