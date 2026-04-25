package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.ConceptRelationshipEntity;
import com.maidc.data.service.ConceptMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/masterdata/mappings")
@RequiredArgsConstructor
public class MappingController {

    private final ConceptMappingService service;

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping
    public R<ConceptRelationshipEntity> createMapping(@RequestBody Map<String, Object> body) {
        Long sourceId = toLong(body.get("sourceId"));
        Long targetId = toLong(body.get("targetId"));
        String type = (String) body.get("type");
        return R.ok(service.createMapping(sourceId, targetId, type));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping("/batch")
    public R<List<ConceptRelationshipEntity>> batchCreate(@RequestBody List<ConceptRelationshipEntity> entities) {
        return R.ok(service.batchCreateMappings(entities));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
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
