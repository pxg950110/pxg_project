package com.maidc.data.repository;

import com.maidc.data.entity.ClinicalNoteEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ClinicalNoteSpecification {

    private ClinicalNoteSpecification() {}

    public static Specification<ClinicalNoteEntity> buildSearchSpec(
            Long encounterId, Long patientId, String noteType,
            String noteCategory, String signStatus, String urgency,
            String source, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (encounterId != null) predicates.add(cb.equal(root.get("encounterId"), encounterId));
            if (patientId != null) predicates.add(cb.equal(root.get("patientId"), patientId));
            if (noteType != null && !noteType.isBlank()) predicates.add(cb.equal(root.get("noteType"), noteType));
            if (noteCategory != null && !noteCategory.isBlank()) predicates.add(cb.equal(root.get("noteCategory"), noteCategory));
            if (signStatus != null && !signStatus.isBlank()) predicates.add(cb.equal(root.get("signStatus"), signStatus));
            if (urgency != null && !urgency.isBlank()) predicates.add(cb.equal(root.get("urgency"), urgency));
            if (source != null && !source.isBlank()) predicates.add(cb.equal(root.get("source"), source));
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), like),
                        cb.like(root.get("content"), like),
                        cb.like(root.get("author"), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
