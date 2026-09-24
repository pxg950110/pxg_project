package com.maidc.data.repository.rdr;

import com.maidc.data.entity.rdr.RdrExtractionExecutionEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class RdrExtractionExecutionSpecification {

    private RdrExtractionExecutionSpecification() {
        // utility class
    }

    public static Specification<RdrExtractionExecutionEntity> buildSearchSpec(
            Long taskId, String status, String executionType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (taskId != null) {
                predicates.add(cb.equal(root.get("taskId"), taskId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (executionType != null && !executionType.isBlank()) {
                predicates.add(cb.equal(root.get("executionType"), executionType));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
