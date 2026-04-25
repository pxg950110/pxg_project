package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.ConceptAncestorEntity;
import com.maidc.data.entity.ConceptRelationshipEntity;
import com.maidc.data.entity.ConceptSynonymEntity;
import com.maidc.data.service.ConceptMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/concepts")
@RequiredArgsConstructor
public class ConceptMappingController {

    private final ConceptMappingService service;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/children")
    public R<List<ConceptRelationshipEntity>> getChildren(@PathVariable Long id) {
        return R.ok(service.getChildren(id));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/descendants")
    public R<List<ConceptAncestorEntity>> getDescendants(@PathVariable Long id) {
        return R.ok(service.getDescendants(id));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/ancestors")
    public R<List<ConceptAncestorEntity>> getAncestors(@PathVariable Long id) {
        return R.ok(service.getAncestors(id));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/mappings")
    public R<List<ConceptRelationshipEntity>> getMappings(@PathVariable Long id) {
        return R.ok(service.getMappings(id));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/synonyms")
    public R<List<ConceptSynonymEntity>> getSynonyms(@PathVariable Long id) {
        return R.ok(service.getSynonyms(id));
    }
}
