package com.maidc.audit.repository;

import com.maidc.audit.entity.DataAccessLogEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DataAccessLogSpecification {

    private DataAccessLogSpecification() {
        // utility class
    }

    public static Specification<DataAccessLogEntity> buildSearchSpec(String userId, String dataType,
                                                                      String patientId, LocalDateTime startTime,
                                                                      LocalDateTime endTime) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null && !userId.isBlank()) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (dataType != null && !dataType.isBlank()) {
                predicates.add(cb.equal(root.get("dataType"), dataType));
            }
            if (patientId != null && !patientId.isBlank()) {
                predicates.add(cb.equal(root.get("patientId"), patientId));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endTime));
            }

            // default sort by createdAt descending
            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
