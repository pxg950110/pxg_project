package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.SyncTaskEntity;
import com.maidc.data.service.SyncTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cdr/sync-tasks")
@RequiredArgsConstructor
public class SyncTaskController {

    private final SyncTaskService syncTaskService;

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping
    public R<Page<SyncTaskEntity>> listSyncTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(syncTaskService.listSyncTasks(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}")
    public R<SyncTaskEntity> getSyncTask(@PathVariable Long id) {
        return R.ok(syncTaskService.getSyncTask(id));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}/logs")
    public R<List<Map<String, Object>>> getSyncTaskLogs(@PathVariable Long id) {
        return R.ok(List.of());
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/{id}/retry")
    public R<SyncTaskEntity> retrySyncTask(@PathVariable Long id) {
        return R.ok(syncTaskService.updateSyncTaskStatus(id, "PENDING"));
    }
}
