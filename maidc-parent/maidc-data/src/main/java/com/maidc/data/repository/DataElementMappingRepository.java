package com.maidc.data.repository;

import com.maidc.data.entity.DataElementMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataElementMappingRepository extends JpaRepository<DataElementMappingEntity, Long> {

    List<DataElementMappingEntity> findByDataElementIdAndIsDeletedFalse(Long dataElementId);

    Optional<DataElementMappingEntity> findByIdAndIsDeletedFalse(Long id);
}
