package com.maidc.data.repository;

import com.maidc.data.entity.GenomicVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GenomicVariantRepository extends JpaRepository<GenomicVariantEntity, Long>,
        JpaSpecificationExecutor<GenomicVariantEntity> {
}
