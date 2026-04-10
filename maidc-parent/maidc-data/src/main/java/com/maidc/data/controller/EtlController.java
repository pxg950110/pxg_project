package com.maidc.data.controller;

import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.data.dto.EtlTaskCreateDTO;
import com.maidc.data.dto.EtlTaskQueryDTO;
import com.maidc.data.service.EtlTaskService;
import com.maidc.data.vo.EtlTaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/etl")
@RequiredArgsConstructor
public class EtlController {

    private final EtlTaskService etlTaskService;

    @PreAuthorize("hasPermission('etl:read')")
    @GetMapping("/tasks")
    public R<PageResult<EtlTaskVO>> listTasks(EtlTaskQueryDTO query) {
        return R.ok(etlTaskService.listTasks(query));
    }

    @OperLog(module = "etl", operation = "createTask")
    @PreAuthorize("hasPermission('etl:create')")
    @PostMapping("/tasks")
    public R<EtlTaskVO> createTask(@RequestBody @Valid EtlTaskCreateDTO dto) {
        return R.ok(etlTaskService.createTask(dto));
    }

    @PreAuthorize("hasPermission('etl:read')")
    @GetMapping("/tasks/{id}")
    public R<EtlTaskVO> getTask(@PathVariable Long id) {
        return R.ok(etlTaskService.getTask(id));
    }

    @OperLog(module = "etl", operation = "triggerTask")
    @PreAuthorize("hasPermission('etl:create')")
    @PostMapping("/tasks/{id}/trigger")
    public R<EtlTaskVO> triggerTask(@PathVariable Long id) {
        return R.ok(etlTaskService.triggerTask(id));
    }

    @OperLog(module = "etl", operation = "pauseTask")
    @PreAuthorize("hasPermission('etl:create')")
    @PutMapping("/tasks/{id}/pause")
    public R<EtlTaskVO> pauseTask(@PathVariable Long id) {
        return R.ok(etlTaskService.pauseTask(id));
    }

    @OperLog(module = "etl", operation = "deleteTask")
    @PreAuthorize("hasPermission('etl:create')")
    @DeleteMapping("/tasks/{id}")
    public R<Void> deleteTask(@PathVariable Long id) {
        etlTaskService.deleteTask(id);
        return R.ok();
    }
}
