package com.maidc.data.repository;

import com.maidc.data.entity.ImagingFindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagingFindingRepository extends JpaRepository<ImagingFindingEntity, Long>, JpaSpecificationExecutor<ImagingFindingEntity> {
}
