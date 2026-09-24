package com.maidc.data.repository.cdr;

import com.maidc.data.entity.cdr.CdrQualityRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CDR数据质量规则Repository
 */
@Repository
public interface CdrQualityRuleRepository extends
        JpaRepository<CdrQualityRuleEntity, Long>,
        JpaSpecificationExecutor<CdrQualityRuleEntity> {

    Optional<CdrQualityRuleEntity> findByRuleCodeAndIsDeletedFalse(String ruleCode);

    List<CdrQualityRuleEntity> findByTargetSchemaAndTargetTableAndIsDeletedFalse(String schema, String table);

    List<CdrQualityRuleEntity> findByEnabledAndIsDeletedFalse(Boolean enabled);

    List<CdrQualityRuleEntity> findByRuleCategoryAndIsDeletedFalse(String category);

    boolean existsByRuleCodeAndIsDeletedFalse(String ruleCode);

    @Modifying
    @Query("UPDATE CdrQualityRuleEntity r SET r.executionCount = r.executionCount + 1, " +
           "r.lastExecutionAt = CURRENT_TIMESTAMP WHERE r.id = ?1")
    void incrementExecutionCount(Long ruleId);

    @Query("SELECT DISTINCT r.ruleCategory FROM CdrQualityRuleEntity r WHERE r.isDeleted = false")
    List<String> findDistinctCategories();

    @Query("SELECT COUNT(r) FROM CdrQualityRuleEntity r WHERE r.enabled = true AND r.isDeleted = false")
    long countEnabledRules();

    Optional<CdrQualityRuleEntity> findByIdAndIsDeletedFalse(Long id);
}