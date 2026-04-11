package com.maidc.data.repository;

import com.maidc.data.entity.ResearchCohortEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResearchCohortRepository extends JpaRepository<ResearchCohortEntity, Long>,
        JpaSpecificationExecutor<ResearchCohortEntity> {

    List<ResearchCohortEntity> findByProjectIdAndIsDeletedFalse(Long projectId);
}
