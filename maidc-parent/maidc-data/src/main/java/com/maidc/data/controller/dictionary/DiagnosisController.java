package com.maidc.data.controller.dictionary;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.dictionary.DiagnosisEntity;
import com.maidc.data.service.dictionary.DiagnosisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import com.maidc.common.security.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/diagnoses")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService service;

    @GetMapping
    public R<Page<DiagnosisEntity>> list(
            @RequestParam(required = false) String chapterCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(chapterCode, status, keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public R<DiagnosisEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @GetMapping("/by-icd10/{icd10Code}")
    public R<DiagnosisEntity> getByIcd10Code(@PathVariable String icd10Code) {
        return R.ok(service.getByIcd10Code(icd10Code));
    }

    @GetMapping("/search")
    public R<Page<DiagnosisEntity>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.search(keyword, page, pageSize));
    }

    @GetMapping("/tree")
    public R<List<DiagnosisEntity>> getTree() {
        return R.ok(service.getTree());
    }

    @GetMapping("/chapter/{chapterCode}")
    public R<List<DiagnosisEntity>> getByChapter(@PathVariable String chapterCode) {
        return R.ok(service.getByChapter(chapterCode));
    }

    @GetMapping("/{parentId}/children")
    public R<List<DiagnosisEntity>> getChildren(@PathVariable Long parentId) {
        return R.ok(service.getChildren(parentId));
    }

    @RequirePermission("masterdata:create")
    @PostMapping
    public R<DiagnosisEntity> create(@RequestBody DiagnosisEntity entity) {
        return R.ok(service.create(entity));
    }

    @RequirePermission("masterdata:update")
    @PutMapping("/{id}")
    public R<DiagnosisEntity> update(@PathVariable Long id, @RequestBody DiagnosisEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @RequirePermission("masterdata:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}