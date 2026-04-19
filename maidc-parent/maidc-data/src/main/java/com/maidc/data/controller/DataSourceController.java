package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.DataSourceEntity;
import com.maidc.data.entity.DataSourceHealthEntity;
import com.maidc.data.entity.SyncTaskEntity;
import com.maidc.data.service.DataSourceHealthService;
import com.maidc.data.service.DataSourceService;
import com.maidc.data.service.SyncTaskService;
import com.maidc.data.service.connection.ConnectionTestResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cdr/datasources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;
    private final SyncTaskService syncTaskService;
    private final DataSourceHealthService dataSourceHealthService;

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping
    public R<Page<DataSourceEntity>> listDataSources(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(dataSourceService.listDataSources(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}")
    public R<DataSourceEntity> getDataSource(@PathVariable Long id) {
        return R.ok(dataSourceService.getDataSource(id));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping
    public R<DataSourceEntity> createDataSource(@RequestBody DataSourceEntity entity) {
        return R.ok(dataSourceService.createDataSource(entity));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PutMapping("/{id}")
    public R<DataSourceEntity> updateDataSource(@PathVariable Long id, @RequestBody DataSourceEntity entity) {
        return R.ok(dataSourceService.updateDataSource(id, entity));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/{id}")
    public R<Void> deleteDataSource(@PathVariable Long id) {
        dataSourceService.deleteDataSource(id);
        return R.ok();
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/{id}/test-connection")
    public R<Map<String, Object>> testConnection(@PathVariable Long id) {
        ConnectionTestResult result = dataSourceService.testSavedConnection(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", result.success());
        resp.put("message", result.message());
        resp.put("latencyMs", result.latencyMs());
        resp.put("details", result.details());
        return R.ok(resp);
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/test-connection")
    public R<Map<String, Object>> testConnectionPreSave(@RequestBody Map<String, Object> body) {
        String typeCode = (String) body.get("type_code");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) body.get("connection_params");
        ConnectionTestResult result = dataSourceService.testConnection(typeCode, params);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", result.success());
        resp.put("message", result.message());
        resp.put("latencyMs", result.latencyMs());
        resp.put("details", result.details());
        return R.ok(resp);
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}/health")
    public R<List<DataSourceHealthEntity>> getHealthHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "50") int limit) {
        return R.ok(dataSourceHealthService.getRecentHealth(id, limit));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}/health/stats")
    public R<Map<String, Object>> getHealthStats(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int days) {
        return R.ok(dataSourceHealthService.getHealthStats(id, days));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/{id}/sync")
    public R<SyncTaskEntity> triggerSync(@PathVariable Long id) {
        SyncTaskEntity task = new SyncTaskEntity();
        task.setSourceId(id);
        task.setTaskName("手动同步-" + id);
        task.setSyncType("FULL");
        task.setStatus("PENDING");
        return R.ok(syncTaskService.createSyncTask(task));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}/sync-history")
    public R<List<SyncTaskEntity>> getSyncHistory(@PathVariable Long id) {
        return R.ok(syncTaskService.getSyncTasksBySourceId(id));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}/schema-mapping")
    public R<Map<String, Object>> getSchemaMapping(@PathVariable Long id) {
        return R.ok(Map.of("tables", List.of(), "mappings", List.of()));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}/statistics")
    public R<Map<String, Object>> getStatistics(@PathVariable Long id) {
        return R.ok(Map.of("totalRecords", 0, "lastSyncTime", ""));
    }
}
