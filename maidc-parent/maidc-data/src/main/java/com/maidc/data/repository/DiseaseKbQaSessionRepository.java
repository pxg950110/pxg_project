package com.maidc.data.repository;

import com.maidc.data.entity.DiseaseKbQaSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiseaseKbQaSessionRepository extends JpaRepository<DiseaseKbQaSessionEntity, Long> {

    List<DiseaseKbQaSessionEntity> findBySpaceIdAndIsDeletedFalseOrderByCreatedAtDesc(Long spaceId);
}
