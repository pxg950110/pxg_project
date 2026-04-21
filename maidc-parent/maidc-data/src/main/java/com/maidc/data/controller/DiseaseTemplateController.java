package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.DiseaseTemplateEntity;
import com.maidc.data.repository.DiseaseTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dict/disease-templates")
@RequiredArgsConstructor
public class DiseaseTemplateController {

    private final DiseaseTemplateRepository repository;

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping
    public R<List<DiseaseTemplateEntity>> search(@RequestParam(required = false) String q) {
        if (q == null || q.isBlank()) {
            return R.ok(repository.findByIsDeletedFalse(PageRequest.of(0, 20)).getContent());
        }
        return R.ok(repository.findByDiseaseNameContainingAndIsDeletedFalse(q));
    }
}
