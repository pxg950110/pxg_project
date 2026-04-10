package com.maidc.audit.repository;

import com.maidc.audit.entity.AuditLogEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogSpecification {

    private AuditLogSpecification() {
        // utility class
    }

    public static Specification<AuditLogEntity> buildSearchSpec(String module, String operation,
                                                                 String username, LocalDateTime startTime,
                                                                 LocalDateTime endTime, Short status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (module != null && !module.isBlank()) {
                predicates.add(cb.equal(root.get("module"), module));
            }
            if (operation != null && !operation.isBlank()) {
                predicates.add(cb.like(root.get("operation"), "%" + operation + "%"));
            }
            if (username != null && !username.isBlank()) {
                predicates.add(cb.like(root.get("username"), "%" + username + "%"));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endTime));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // default sort by createdAt descending
            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
