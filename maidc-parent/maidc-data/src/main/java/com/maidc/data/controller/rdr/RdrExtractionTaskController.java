package com.maidc.data.controller.rdr;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.rdr.RdrExtractionTaskEntity;
import com.maidc.data.service.rdr.RdrExtractionTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RDR数据抽取任务Controller
 */
@RestController
@RequestMapping("/api/v1/rdr/extraction-tasks")
@RequiredArgsConstructor
public class RdrExtractionTaskController {

    private final RdrExtractionTaskService service;

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping
    public R<Page<RdrExtractionTaskEntity>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String extractionType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(projectId, extractionType, status, keyword, page, pageSize));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/{id}")
    public R<RdrExtractionTaskEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/code/{taskCode}")
    public R<RdrExtractionTaskEntity> getByCode(@PathVariable String taskCode) {
        return R.ok(service.getByCode(taskCode));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/project/{projectId}")
    public R<List<RdrExtractionTaskEntity>> getByProject(@PathVariable Long projectId) {
        return R.ok(service.getByProject(projectId));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/scheduled")
    public R<List<RdrExtractionTaskEntity>> getScheduledActiveTasks() {
        return R.ok(service.getActiveScheduledTasks());
    }

    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping
    public R<RdrExtractionTaskEntity> create(@RequestBody RdrExtractionTaskEntity entity) {
        return R.ok(service.create(entity));
    }

    @PreAuthorize("hasPermission('rdr:update')")
    @PutMapping("/{id}")
    public R<RdrExtractionTaskEntity> update(@PathVariable Long id, @RequestBody RdrExtractionTaskEntity entity) {
        return R.ok(service.update(id, entity));
    }

    @PreAuthorize("hasPermission('rdr:update')")
    @PostMapping("/{id}/activate")
    public R<RdrExtractionTaskEntity> activate(@PathVariable Long id) {
        return R.ok(service.activate(id));
    }

    @PreAuthorize("hasPermission('rdr:update')")
    @PostMapping("/{id}/pause")
    public R<RdrExtractionTaskEntity> pause(@PathVariable Long id) {
        return R.ok(service.pause(id));
    }

    @PreAuthorize("hasPermission('rdr:update')")
    @PostMapping("/{id}/archive")
    public R<RdrExtractionTaskEntity> archive(@PathVariable Long id) {
        return R.ok(service.archive(id));
    }

    @PreAuthorize("hasPermission('rdr:delete')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        return R.ok(service.getStats());
    }
}