package com.maidc.task.controller;

import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.task.dto.TaskCreateDTO;
import com.maidc.task.dto.TaskUpdateDTO;
import com.maidc.task.service.TaskService;
import com.maidc.task.vo.TaskExecutionVO;
import com.maidc.task.vo.TaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/task/schedules")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PreAuthorize("hasPermission('task:read')")
    @GetMapping
    public R<PageResult<TaskVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(taskService.listTasks(status, taskType, page, pageSize));
    }

    @OperLog(module = "task", operation = "createTask")
    @PreAuthorize("hasPermission('task:write')")
    @PostMapping
    public R<TaskVO> create(@RequestBody @Valid TaskCreateDTO dto) {
        return R.ok(taskService.createTask(dto));
    }

    @PreAuthorize("hasPermission('task:read')")
    @GetMapping("/{id}")
    public R<TaskVO> get(@PathVariable String id) {
        return R.ok(taskService.getTask(id));
    }

    @OperLog(module = "task", operation = "updateTask")
    @PreAuthorize("hasPermission('task:write')")
    @PutMapping("/{id}")
    public R<TaskVO> update(@PathVariable String id, @RequestBody TaskUpdateDTO dto) {
        return R.ok(taskService.updateTask(id, dto));
    }

    @OperLog(module = "task", operation = "deleteTask")
    @PreAuthorize("hasPermission('task:write')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        taskService.deleteTask(id);
        return R.ok();
    }

    @OperLog(module = "task", operation = "triggerTask")
    @PreAuthorize("hasPermission('task:execute')")
    @PostMapping("/{id}/trigger")
    public R<TaskVO> trigger(@PathVariable String id) {
        return R.ok(taskService.triggerTask(id));
    }

    @OperLog(module = "task", operation = "pauseTask")
    @PreAuthorize("hasPermission('task:execute')")
    @PutMapping("/{id}/pause")
    public R<TaskVO> pause(@PathVariable String id) {
        return R.ok(taskService.pauseTask(id));
    }

    @OperLog(module = "task", operation = "resumeTask")
    @PreAuthorize("hasPermission('task:execute')")
    @PutMapping("/{id}/resume")
    public R<TaskVO> resume(@PathVariable String id) {
        return R.ok(taskService.resumeTask(id));
    }

    @PreAuthorize("hasPermission('task:read')")
    @GetMapping("/{id}/executions")
    public R<PageResult<TaskExecutionVO>> executions(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(taskService.getExecutions(id, page, pageSize));
    }
}
