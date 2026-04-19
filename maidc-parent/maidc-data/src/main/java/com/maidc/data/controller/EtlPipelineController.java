package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.dto.etl.EtlExecutionQueryDTO;
import com.maidc.data.dto.etl.EtlPipelineCreateDTO;
import com.maidc.data.dto.etl.EtlPipelineQueryDTO;
import com.maidc.data.service.etl.EtlExecutionService;
import com.maidc.data.service.etl.EtlPipelineService;
import com.maidc.data.vo.EtlExecutionVO;
import com.maidc.data.vo.EtlPipelineDetailVO;
import com.maidc.data.vo.EtlPipelineVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cdr/etl/pipelines")
@RequiredArgsConstructor
public class EtlPipelineController {

    private final EtlPipelineService pipelineService;
    private final EtlExecutionService executionService;

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping
    public R<?> listPipelines(EtlPipelineQueryDTO query) {
        return R.ok(pipelineService.listPipelines(query));
    }

    @PreAuthorize("hasPermission('data:create')")
    @PostMapping
    public R<EtlPipelineVO> createPipeline(@RequestBody @Valid EtlPipelineCreateDTO dto) {
        return R.ok(pipelineService.createPipeline(dto));
    }

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping("/{id}")
    public R<EtlPipelineDetailVO> getPipelineDetail(@PathVariable Long id) {
        return R.ok(pipelineService.getPipelineDetail(id));
    }

    @PreAuthorize("hasPermission('data:update')")
    @PutMapping("/{id}")
    public R<EtlPipelineVO> updatePipeline(@PathVariable Long id, @RequestBody @Valid EtlPipelineCreateDTO dto) {
        return R.ok(pipelineService.updatePipeline(id, dto));
    }

    @PreAuthorize("hasPermission('data:delete')")
    @DeleteMapping("/{id}")
    public R<Void> deletePipeline(@PathVariable Long id) {
        pipelineService.deletePipeline(id);
        return R.ok();
    }

    @PreAuthorize("hasPermission('data:create')")
    @PostMapping("/{id}/run")
    public R<EtlExecutionVO> runPipeline(@PathVariable Long id) {
        return R.ok(executionService.triggerExecution(id, "MANUAL"));
    }

    @PreAuthorize("hasPermission('data:read')")
    @PostMapping("/{id}/validate")
    public R<List<String>> validatePipeline(@PathVariable Long id) {
        return R.ok(pipelineService.validatePipeline(id));
    }

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping("/{id}/executions")
    public R<?> listExecutions(@PathVariable Long id, EtlExecutionQueryDTO query) {
        query.setPipelineId(id);
        return R.ok(executionService.listExecutions(query));
    }

    @PreAuthorize("hasPermission('data:create')")
    @PostMapping("/{id}/copy")
    public R<EtlPipelineVO> copyPipeline(@PathVariable Long id) {
        return R.ok(pipelineService.copyPipeline(id));
    }

    @PreAuthorize("hasPermission('data:update')")
    @PutMapping("/{id}/status")
    public R<EtlPipelineVO> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(pipelineService.updateStatus(id, body.get("status")));
    }
}
