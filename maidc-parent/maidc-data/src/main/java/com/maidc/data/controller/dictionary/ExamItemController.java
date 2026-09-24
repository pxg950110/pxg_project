package com.maidc.data.controller.dictionary;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.dictionary.ExamItemEntity;
import com.maidc.data.service.dictionary.ExamItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import com.maidc.common.security.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/exam-items")
@RequiredArgsConstructor
public class ExamItemController {

    private final ExamItemService service;

    @GetMapping
    public R<Page<ExamItemEntity>> list(
            @RequestParam(required = false) String examType,
            @RequestParam(required = false) String bodySite,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(examType, bodySite, keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public R<ExamItemEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @GetMapping("/search")
    public R<Page<ExamItemEntity>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.search(keyword, page, pageSize));
    }

    @GetMapping("/exam-types")
    public R<List<String>> getAllExamTypes() {
        return R.ok(service.getAllExamTypes());
    }

    @RequirePermission("masterdata:create")
    @PostMapping
    public R<ExamItemEntity> create(@RequestBody ExamItemEntity entity) {
        return R.ok(service.create(entity));
    }

    @RequirePermission("masterdata:update")
    @PutMapping("/{id}")
    public R<ExamItemEntity> update(@PathVariable Long id, @RequestBody ExamItemEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @RequirePermission("masterdata:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}