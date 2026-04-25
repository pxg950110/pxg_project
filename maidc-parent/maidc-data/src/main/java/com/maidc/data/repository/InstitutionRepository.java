package com.maidc.data.repository;

import com.maidc.data.entity.InstitutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstitutionRepository extends JpaRepository<InstitutionEntity, Long>, JpaSpecificationExecutor<InstitutionEntity> {

    Optional<InstitutionEntity> findByInstCodeAndIsDeletedFalse(String instCode);

    boolean existsByInstCodeAndIsDeletedFalse(String instCode);
}
