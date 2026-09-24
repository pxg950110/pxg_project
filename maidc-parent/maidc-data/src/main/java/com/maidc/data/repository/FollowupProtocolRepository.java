package com.maidc.data.repository;

import com.maidc.data.entity.FollowupProtocolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowupProtocolRepository extends JpaRepository<FollowupProtocolEntity, Long> {

    List<FollowupProtocolEntity> findByCohortIdAndIsDeletedFalseOrderByVersionDesc(Long cohortId);

    Optional<FollowupProtocolEntity> findByIdAndIsDeletedFalse(Long id);

    Optional<FollowupProtocolEntity> findByCohortIdAndVersionAndIsDeletedFalse(Long cohortId, Integer version);

    Optional<FollowupProtocolEntity> findFirstByCohortIdAndStatusAndIsDeletedFalseOrderByVersionDesc(Long cohortId, String status);

    @Query("SELECT COUNT(p) FROM com.maidc.data.entity.PatientFollowupEntity p WHERE p.protocolId = :protocolId AND p.isDeleted = false")
    long countFollowupsByProtocolId(@Param("protocolId") Long protocolId);
}
