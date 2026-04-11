package com.maidc.data.repository;

import com.maidc.data.entity.StudySubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudySubjectRepository extends JpaRepository<StudySubjectEntity, Long>,
        JpaSpecificationExecutor<StudySubjectEntity> {

    List<StudySubjectEntity> findByCohortIdAndIsDeletedFalse(Long cohortId);

    List<StudySubjectEntity> findByProjectIdAndIsDeletedFalse(Long projectId);
}
