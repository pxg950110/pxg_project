package com.maidc.data.repository;

import com.maidc.data.entity.PathologyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PathologyRepository extends JpaRepository<PathologyEntity, Long>, JpaSpecificationExecutor<PathologyEntity> {
}
