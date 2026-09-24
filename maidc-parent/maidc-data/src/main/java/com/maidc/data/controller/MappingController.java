package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.ConceptRelationshipEntity;
import com.maidc.data.service.ConceptMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import com.maidc.common.security.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/masterdata/mappings")
@RequiredArgsConstructor
public class MappingController {

    private final ConceptMappingService service;

    @RequirePermission("masterdata:read")
    @GetMapping
    public R<Page<ConceptRelationshipEntity>> listMappings(
            @RequestParam(required = false) Long conceptId,
            @RequestParam(required = false) String relationshipType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Specification<ConceptRelationshipEntity> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (conceptId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("conceptId1"), conceptId),
                        cb.equal(root.get("conceptId2"), conceptId)
                ));
            }
            if (relationshipType != null && !relationshipType.isBlank()) {
                predicates.add(cb.equal(root.get("relationshipType"), relationshipType));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Page<ConceptRelationshipEntity> result = service.listMappings(spec, PageRequest.of(page - 1, size));
        return R.ok(result);
    }

    @RequirePermission("masterdata:create")
    @PostMapping
    public R<ConceptRelationshipEntity> createMapping(@RequestBody Map<String, Object> body) {
        Long sourceId = toLong(body.get("sourceId"));
        Long targetId = toLong(body.get("targetId"));
        String type = (String) body.get("type");
        return R.ok(service.createMapping(sourceId, targetId, type));
    }

    @RequirePermission("masterdata:create")
    @PostMapping("/batch")
    public R<List<ConceptRelationshipEntity>> batchCreate(@RequestBody List<ConceptRelationshipEntity> entities) {
        return R.ok(service.batchCreateMappings(entities));
    }

    @RequirePermission("masterdata:delete")
    @DeleteMapping("/{id}")
    public R<Void> deleteMapping(@PathVariable Long id) {
        service.deleteMapping(id);
        return R.ok();
    }

    private Long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        return null;
    }
}
