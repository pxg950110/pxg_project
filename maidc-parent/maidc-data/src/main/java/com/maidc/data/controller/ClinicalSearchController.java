package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.dto.ClinicalSearchRequest;
import com.maidc.data.dto.ClinicalSearchResult;
import com.maidc.data.service.ClinicalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cdr/search")
@RequiredArgsConstructor
public class ClinicalSearchController {

    private final ClinicalSearchService clinicalSearchService;

    @PreAuthorize("hasPermission('cdr:read')")
    @PostMapping
    public R<ClinicalSearchResult> search(@RequestBody ClinicalSearchRequest request) {
        if (request.getDomain() == null) {
            return R.fail(400, "搜索域(domain)不能为空");
        }
        return R.ok(clinicalSearchService.search(request));
    }
}
