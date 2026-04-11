package com.maidc.label.controller;

import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.label.dto.LabelTaskCreateDTO;
import com.maidc.label.dto.LabelTaskUpdateDTO;
import com.maidc.label.service.LabelTaskService;
import com.maidc.label.vo.LabelStatsVO;
import com.maidc.label.vo.LabelTaskDetailVO;
import com.maidc.label.vo.LabelTaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/label/tasks")
@RequiredArgsConstructor
public class LabelTaskController {

    private final LabelTaskService labelTaskService;

    /**
     * List label tasks with optional filters
     */
    @PreAuthorize("hasPermission('label:read')")
    @GetMapping
    public R<PageResult<LabelTaskVO>> listTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(labelTaskService.listTasks(status, taskType, page, pageSize));
    }

    /**
     * Create a new label task
     */
    @PreAuthorize("hasPermission('label:write')")
    @PostMapping
    public R<LabelTaskVO> createTask(@Valid @RequestBody LabelTaskCreateDTO dto) {
        return R.ok(labelTaskService.createTask(dto));
    }

    /**
     * Get task detail by id
     */
    @PreAuthorize("hasPermission('label:read')")
    @GetMapping("/{id}")
    public R<LabelTaskDetailVO> getTask(@PathVariable Long id) {
        return R.ok(labelTaskService.getTask(id));
    }

    /**
     * Update a label task
     */
    @PreAuthorize("hasPermission('label:write')")
    @PutMapping("/{id}")
    public R<LabelTaskVO> updateTask(@PathVariable Long id,
                                     @RequestBody LabelTaskUpdateDTO dto) {
        return R.ok(labelTaskService.updateTask(id, dto));
    }

    /**
     * Soft delete a label task
     */
    @PreAuthorize("hasPermission('label:write')")
    @DeleteMapping("/{id}")
    public R<Void> deleteTask(@PathVariable Long id) {
        labelTaskService.deleteTask(id);
        return R.ok();
    }

    /**
     * Get task statistics
     */
    @PreAuthorize("hasPermission('label:read')")
    @GetMapping("/{id}/stats")
    public R<LabelStatsVO> getTaskStats(@PathVariable Long id) {
        return R.ok(labelTaskService.getTaskStats(id));
    }

    /**
     * Trigger AI pre-annotation for a task
     */
    @PreAuthorize("hasPermission('label:write')")
    @PostMapping("/{id}/ai-preannotate")
    public R<Void> triggerAiPreAnnotate(@PathVariable Long id) {
        labelTaskService.triggerAiPreAnnotate(id);
        return R.ok();
    }
}
