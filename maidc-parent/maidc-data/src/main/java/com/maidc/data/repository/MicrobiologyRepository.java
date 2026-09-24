package com.maidc.data.repository;

import com.maidc.data.entity.MicrobiologyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MicrobiologyRepository extends JpaRepository<MicrobiologyEntity, Long>, JpaSpecificationExecutor<MicrobiologyEntity> {
    List<MicrobiologyEntity> findByPatientIdAndIsDeletedFalse(Long patientId);
}
