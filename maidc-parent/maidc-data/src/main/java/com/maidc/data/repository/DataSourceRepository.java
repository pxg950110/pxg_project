package com.maidc.data.repository;

import com.maidc.data.entity.DataSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSourceEntity, Long>,
        JpaSpecificationExecutor<DataSourceEntity> {
}
