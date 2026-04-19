package com.maidc.data.repository;

import com.maidc.data.entity.DataSourceTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DataSourceTypeRepository extends JpaRepository<DataSourceTypeEntity, Long> {
    Optional<DataSourceTypeEntity> findByTypeCode(String typeCode);
}