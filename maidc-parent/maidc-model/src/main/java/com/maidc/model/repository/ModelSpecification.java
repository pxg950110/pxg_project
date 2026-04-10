package com.maidc.model.repository;

import com.maidc.model.entity.ModelEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ModelSpecification {

    private ModelSpecification() {
        // utility class
    }

    public static Specification<ModelEntity> buildSearchSpec(Long orgId, String keyword, String modelType, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            predicates.add(cb.equal(root.get("orgId"), orgId));
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.or(
                        cb.like(root.get("modelName"), "%" + keyword + "%"),
                        cb.like(root.get("modelCode"), "%" + keyword + "%")
                ));
            }
            if (modelType != null) {
                predicates.add(cb.equal(root.get("modelType"), modelType));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
