package com.maidc.data.repository;

import com.maidc.data.entity.DataElementValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataElementValueRepository extends JpaRepository<DataElementValueEntity, Long> {

    List<DataElementValueEntity> findByDataElementIdAndIsDeletedFalseOrderBySortOrder(Long dataElementId);

    void deleteByDataElementId(Long dataElementId);
}
