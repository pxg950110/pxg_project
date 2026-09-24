package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.KnowledgeCategoryEntity;
import com.maidc.data.entity.KnowledgeItemEntity;
import com.maidc.data.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/masterdata/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService service;

    // ── Categories ──

    @GetMapping("/categories")
    public R<List<KnowledgeCategoryEntity>> listCategories() {
        return R.ok(service.listCategories());
    }

    @PostMapping("/categories")
    public R<KnowledgeCategoryEntity> createCategory(@RequestBody KnowledgeCategoryEntity entity) {
        if (entity.getName() == null || entity.getName().isBlank()) {
            return R.fail(400, "分类名称不能为空");
        }
        return R.ok(service.createCategory(entity));
    }

    @PutMapping("/categories/{id}")
    public R<KnowledgeCategoryEntity> updateCategory(@PathVariable Long id, @RequestBody KnowledgeCategoryEntity entity) {
        return R.ok(service.updateCategory(id, entity));
    }

    @DeleteMapping("/categories/{id}")
    public R<Void> deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
        return R.ok(null);
    }

    // ── Knowledge Items ──

    @GetMapping
    public R<Page<KnowledgeItemEntity>> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        return R.ok(service.search(categoryId, itemType, keyword, page, page_size));
    }

    @GetMapping("/{id}")
    public R<KnowledgeItemEntity> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @PostMapping
    public R<KnowledgeItemEntity> create(@RequestBody KnowledgeItemEntity entity) {
        if (entity.getTitle() == null || entity.getTitle().isBlank()) {
            return R.fail(400, "标题不能为空");
        }
        return R.ok(service.create(entity));
    }

    @PutMapping("/{id}")
    public R<KnowledgeItemEntity> update(@PathVariable Long id, @RequestBody KnowledgeItemEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok(null);
    }

    // ── Concept Associations ──

    @GetMapping("/{id}/concepts")
    public R<List<Map<String, Object>>> getConcepts(@PathVariable Long id) {
        return R.ok(service.getConceptAssociations(id));
    }

    @GetMapping("/by-concept/{conceptId}")
    public R<List<Map<String, Object>>> getByConcept(@PathVariable Long conceptId) {
        return R.ok(service.getKnowledgeByConcept(conceptId));
    }

    @PostMapping("/{id}/concepts")
    public R<Void> associateConcept(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long conceptId = Long.valueOf(body.get("conceptId").toString());
        String relevance = (String) body.getOrDefault("relevance", "RELATED");
        service.associateConcept(id, conceptId, relevance);
        return R.ok(null);
    }

    @DeleteMapping("/associations/{id}")
    public R<Void> removeAssociation(@PathVariable Long id) {
        service.removeAssociation(id);
        return R.ok(null);
    }
}
