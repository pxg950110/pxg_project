package com.maidc.data.repository;

import com.maidc.data.entity.DiseaseKbQaMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiseaseKbQaMessageRepository extends JpaRepository<DiseaseKbQaMessageEntity, Long> {

    List<DiseaseKbQaMessageEntity> findBySessionIdAndIsDeletedFalseOrderByCreatedAtAsc(Long sessionId);

    long countBySessionId(Long sessionId);
}
