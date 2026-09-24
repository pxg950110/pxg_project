package com.maidc.data.repository.rdr;

import com.maidc.data.entity.rdr.RdrExtractionTaskEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class RdrExtractionTaskSpecification {

    private RdrExtractionTaskSpecification() {
        // utility class
    }

    public static Specification<RdrExtractionTaskEntity> buildSearchSpec(
            Long projectId, String extractionType, String status, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (projectId != null) {
                predicates.add(cb.equal(root.get("projectId"), projectId));
            }
            if (extractionType != null && !extractionType.isBlank()) {
                predicates.add(cb.equal(root.get("extractionType"), extractionType));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.or(
                        cb.like(root.get("taskCode"), "%" + keyword + "%"),
                        cb.like(root.get("taskName"), "%" + keyword + "%")
                ));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
