package com.maidc.data.repository;

import com.maidc.data.entity.DiseaseKbSpaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiseaseKbSpaceRepository extends JpaRepository<DiseaseKbSpaceEntity, Long>, JpaSpecificationExecutor<DiseaseKbSpaceEntity> {

    boolean existsByNameAndIsDeletedFalse(String name);

    boolean existsByNameAndIsDeletedFalseAndIdNot(String name, Long id);

    Optional<DiseaseKbSpaceEntity> findByCohortIdAndIsDeletedFalse(Long cohortId);
}
