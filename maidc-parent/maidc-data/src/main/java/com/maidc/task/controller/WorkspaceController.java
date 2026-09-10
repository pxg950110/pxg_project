package com.maidc.task.controller;

import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.task.service.PersonalTaskService;
import com.maidc.task.service.WorkspaceService;
import com.maidc.task.vo.PersonalTaskVO;
import com.maidc.task.vo.WorkspaceDashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workspace")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final PersonalTaskService personalTaskService;

    @PreAuthorize("hasPermission('workspace:read')")
    @GetMapping("/dashboard")
    public R<WorkspaceDashboardVO> getDashboard(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Org-Id") Long orgId,
            @RequestHeader(value = "X-User-Roles", required = false, defaultValue = "") String rolesHeader,
            @RequestHeader(value = "X-Username", required = false, defaultValue = "") String username) {
        List<String> roles = Arrays.stream(rolesHeader.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        return R.ok(workspaceService.getDashboard(userId, orgId, username, roles));
    }

    @OperLog(module = "workspace", operation = "completeTask")
    @PreAuthorize("hasPermission('workspace:write')")
    @PutMapping("/todos/{id}/complete")
    public R<PersonalTaskVO> completeTask(@PathVariable Long id) {
        return R.ok(personalTaskService.completeTask(id));
    }
}
