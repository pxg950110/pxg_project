package com.maidc.label.controller;

import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.label.dto.LabelTaskCreateDTO;
import com.maidc.label.dto.LabelTaskUpdateDTO;
import com.maidc.label.repository.LabelTaskRepository;
import com.maidc.label.service.LabelTaskService;
import com.maidc.label.vo.LabelStatsVO;
import com.maidc.label.vo.LabelTaskDetailVO;
import com.maidc.label.vo.LabelTaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/label/tasks")
@RequiredArgsConstructor
public class LabelTaskController {

    private final LabelTaskService labelTaskService;
    private final LabelTaskRepository labelTaskRepository;

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
     * Get task summary counts（前端 label.ts 契约键：totalTasks/inProgress/labeledData/avgConsistency）
     */
    @PreAuthorize("hasPermission('label:read')")
    @GetMapping("/summary")
    public R<Map<String, Object>> getTaskSummary() {
        var tasks = labelTaskRepository.findByIsDeletedFalse();
        long inProgress = tasks.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).count();
        int labeledData = tasks.stream()
                .mapToInt(t -> t.getLabeledCount() != null ? t.getLabeledCount() : 0).sum();
        int verified = tasks.stream()
                .mapToInt(t -> t.getVerifiedCount() != null ? t.getVerifiedCount() : 0).sum();
        // 一致性代理指标：已审核 / 已标注（无标注数据时为 0）
        double avgConsistency = labeledData > 0
                ? Math.round(verified * 10000.0 / labeledData) / 100.0 : 0.0;
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTasks", tasks.size());
        summary.put("inProgress", inProgress);
        summary.put("labeledData", labeledData);
        summary.put("avgConsistency", avgConsistency);
        return R.ok(summary);
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
