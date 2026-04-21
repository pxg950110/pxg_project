package com.maidc.data.repository;

import com.maidc.data.entity.DiseaseCohortPatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiseaseCohortPatientRepository extends JpaRepository<DiseaseCohortPatientEntity, Long> {

    List<DiseaseCohortPatientEntity> findByCohortId(Long cohortId);

    boolean existsByCohortIdAndPatientId(Long cohortId, Long patientId);

    void deleteByCohortIdAndPatientIdAndMatchSource(Long cohortId, Long patientId, String matchSource);

    long countByCohortId(Long cohortId);
}
