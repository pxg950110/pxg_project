package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.dto.SmartSearchRequest;
import com.maidc.data.dto.SmartSearchResult;
import com.maidc.data.service.SmartSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cdr/smart-search")
@RequiredArgsConstructor
public class SmartSearchController {

    private final SmartSearchService smartSearchService;

    @PreAuthorize("hasPermission('cdr:read')")
    @PostMapping
    public R<SmartSearchResult> search(@RequestBody SmartSearchRequest request) {
        if (request.getKeyword() == null || request.getKeyword().isBlank()) {
            return R.fail(400, "搜索关键词不能为空");
        }
        return R.ok(smartSearchService.search(request));
    }
}
