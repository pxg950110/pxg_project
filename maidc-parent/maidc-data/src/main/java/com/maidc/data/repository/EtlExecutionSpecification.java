package com.maidc.data.repository;

import com.maidc.data.entity.EtlExecutionEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EtlExecutionSpecification {

    private EtlExecutionSpecification() {
        // utility class
    }

    public static Specification<EtlExecutionEntity> buildSearchSpec(
            Long pipelineId, Long stepId, String status, String triggerType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (pipelineId != null) {
                predicates.add(cb.equal(root.get("pipelineId"), pipelineId));
            }
            if (stepId != null) {
                predicates.add(cb.equal(root.get("stepId"), stepId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (triggerType != null && !triggerType.isBlank()) {
                predicates.add(cb.equal(root.get("triggerType"), triggerType));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
