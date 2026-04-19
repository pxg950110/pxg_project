package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.dto.etl.EtlExecutionQueryDTO;
import com.maidc.data.service.etl.EtlExecutionService;
import com.maidc.data.vo.EtlExecutionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cdr/etl/executions")
@RequiredArgsConstructor
public class EtlExecutionController {

    private final EtlExecutionService executionService;

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping
    public R<?> listExecutions(EtlExecutionQueryDTO query) {
        return R.ok(executionService.listExecutions(query));
    }

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping("/{id}")
    public R<EtlExecutionVO> getExecution(@PathVariable Long id) {
        return R.ok(executionService.getExecution(id));
    }

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping("/{id}/logs")
    public R<String> getExecutionLogs(@PathVariable Long id) {
        return R.ok(executionService.getExecutionLogs(id));
    }

    @PreAuthorize("hasPermission('data:update')")
    @PostMapping("/{id}/cancel")
    public R<Void> cancelExecution(@PathVariable Long id) {
        executionService.cancelExecution(id);
        return R.ok();
    }

    @PreAuthorize("hasPermission('data:update')")
    @PostMapping("/{id}/retry")
    public R<EtlExecutionVO> retryExecution(@PathVariable Long id) {
        return R.ok(executionService.retryExecution(id));
    }
}
