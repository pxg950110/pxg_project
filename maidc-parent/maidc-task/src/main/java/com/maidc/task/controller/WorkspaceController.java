package com.maidc.task.controller;

import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.task.service.PersonalTaskService;
import com.maidc.task.service.WorkspaceService;
import com.maidc.task.vo.PersonalTaskVO;
import com.maidc.task.vo.WorkspaceDashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workspace")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final PersonalTaskService personalTaskService;

    @PreAuthorize("hasPermission('workspace:read')")
    @GetMapping("/dashboard")
    public R<WorkspaceDashboardVO> getDashboard(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Org-Id", required = false) Long orgId) {
        return R.ok(workspaceService.getDashboard(
                userId != null ? userId : 1L,
                orgId != null ? orgId : 1L));
    }

    @PreAuthorize("hasPermission('workspace:read')")
    @GetMapping("/tasks")
    public R<PageResult<Map<String, Object>>> listTasks(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setItems(Collections.emptyList());
        result.setTotal(0L);
        result.setPage(page);
        result.setPageSize(page_size);
        result.setTotalPages(0);
        return R.ok(result);
    }

    @OperLog(module = "workspace", operation = "completeTask")
    @PreAuthorize("hasPermission('workspace:write')")
    @PutMapping("/todos/{id}/complete")
    public R<PersonalTaskVO> completeTask(@PathVariable Long id) {
        return R.ok(personalTaskService.completeTask(id));
    }
}
