package com.maidc.data.repository;

import com.maidc.data.entity.TerminologyDomainEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TerminologyDomainRepository
        extends JpaRepository<TerminologyDomainEntity, Long>, JpaSpecificationExecutor<TerminologyDomainEntity> {

    Optional<TerminologyDomainEntity> findByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIsDeletedFalse(String code);

    List<TerminologyDomainEntity> findByStatusAndIsDeletedFalseOrderBySortOrder(String status);

    List<TerminologyDomainEntity> findAllByIsDeletedFalseOrderBySortOrder();
}
