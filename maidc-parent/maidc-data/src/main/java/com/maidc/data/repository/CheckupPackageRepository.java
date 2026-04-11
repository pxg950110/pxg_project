package com.maidc.data.repository;

import com.maidc.data.entity.CheckupPackageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckupPackageRepository extends JpaRepository<CheckupPackageEntity, Long>, JpaSpecificationExecutor<CheckupPackageEntity> {
}
