package com.maidc.data.controller.rdr;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.rdr.RdrDatasetVersionEntity;
import com.maidc.data.service.rdr.RdrDatasetVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RDR数据集版本Controller
 */
@RestController
@RequestMapping("/api/v1/rdr/dataset-versions")
@RequiredArgsConstructor
public class RdrDatasetVersionController {

    private final RdrDatasetVersionService service;

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping
    public R<Page<RdrDatasetVersionEntity>> list(
            @RequestParam(required = false) Long datasetId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(datasetId, status, page, pageSize));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/{id}")
    public R<RdrDatasetVersionEntity> get(@PathVariable Long id) {
        service.recordAccess(id);
        return R.ok(service.getById(id));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/code/{versionCode}")
    public R<RdrDatasetVersionEntity> getByCode(@PathVariable String versionCode) {
        return R.ok(service.getByCode(versionCode));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/dataset/{datasetId}")
    public R<List<RdrDatasetVersionEntity>> getByDataset(@PathVariable Long datasetId) {
        return R.ok(service.getByDataset(datasetId));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/dataset/{datasetId}/current")
    public R<RdrDatasetVersionEntity> getCurrentVersion(@PathVariable Long datasetId) {
        RdrDatasetVersionEntity version = service.getCurrentVersion(datasetId);
        return version != null ? R.ok(version) : R.ok(null);
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/draft")
    public R<List<RdrDatasetVersionEntity>> getDraftVersions() {
        return R.ok(service.getDraftVersions());
    }

    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping
    public R<RdrDatasetVersionEntity> create(@RequestBody RdrDatasetVersionEntity entity) {
        return R.ok(service.create(entity));
    }

    @PreAuthorize("hasPermission('rdr:update')")
    @PostMapping("/{id}/publish")
    public R<RdrDatasetVersionEntity> publish(@PathVariable Long id, @RequestParam String publishedBy) {
        return R.ok(service.publish(id, publishedBy));
    }

    @PreAuthorize("hasPermission('rdr:update')")
    @PostMapping("/{id}/archive")
    public R<RdrDatasetVersionEntity> archive(@PathVariable Long id) {
        return R.ok(service.archive(id));
    }

    @PreAuthorize("hasPermission('rdr:update')")
    @PostMapping("/{id}/deprecate")
    public R<RdrDatasetVersionEntity> deprecate(@PathVariable Long id) {
        return R.ok(service.deprecate(id));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        return R.ok(service.getStats());
    }
}