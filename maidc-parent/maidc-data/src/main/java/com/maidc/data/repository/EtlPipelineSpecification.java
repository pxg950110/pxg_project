package com.maidc.data.repository;

import com.maidc.data.entity.EtlPipelineEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EtlPipelineSpecification {

    private EtlPipelineSpecification() {
        // utility class
    }

    public static Specification<EtlPipelineEntity> buildSearchSpec(
            String keyword, Long sourceId, String status, String engineType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("pipelineName")),
                        "%" + keyword.toLowerCase() + "%"));
            }
            if (sourceId != null) {
                predicates.add(cb.equal(root.get("sourceId"), sourceId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (engineType != null && !engineType.isBlank()) {
                predicates.add(cb.equal(root.get("engineType"), engineType));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
