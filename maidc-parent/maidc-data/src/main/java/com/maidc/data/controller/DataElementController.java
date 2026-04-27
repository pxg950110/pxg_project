package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.dto.DataElementCreateDTO;
import com.maidc.data.dto.DataElementMappingDTO;
import com.maidc.data.entity.DataElementEntity;
import com.maidc.data.entity.DataElementMappingEntity;
import com.maidc.data.entity.DataElementValueEntity;
import com.maidc.data.entity.ImportTaskEntity;
import com.maidc.data.service.DataElementImportService;
import com.maidc.data.service.DataElementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/masterdata/data-elements")
@RequiredArgsConstructor
public class DataElementController {

    private final DataElementService service;
    private final DataElementImportService importService;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping
    public R<Page<DataElementEntity>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String registrationStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dataType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(category, registrationStatus, keyword, dataType, page, pageSize));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}")
    public R<DataElementEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping
    public R<DataElementEntity> create(@RequestBody DataElementCreateDTO dto) {
        return R.ok(service.create(dto));
    }

    @PreAuthorize("hasPermission('masterdata:update')")
    @PutMapping("/{id}")
    public R<DataElementEntity> update(@PathVariable Long id, @RequestBody DataElementCreateDTO dto) {
        return R.ok(service.update(id, dto));
    }

    @PreAuthorize("hasPermission('masterdata:delete')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    // ── 允许值 ──

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/values")
    public R<List<DataElementValueEntity>> getValues(@PathVariable Long id) {
        return R.ok(service.getValues(id));
    }

    @PreAuthorize("hasPermission('masterdata:update')")
    @PutMapping("/{id}/values")
    public R<List<DataElementValueEntity>> updateValues(@PathVariable Long id,
                                                         @RequestBody List<DataElementValueEntity> values) {
        return R.ok(service.updateValues(id, values));
    }

    // ── 映射 ──

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/mappings")
    public R<List<DataElementMappingEntity>> getMappings(@PathVariable Long id) {
        return R.ok(service.getMappings(id));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping("/{id}/mappings")
    public R<DataElementMappingEntity> addMapping(@PathVariable Long id,
                                                   @RequestBody DataElementMappingDTO dto) {
        return R.ok(service.addMapping(id, dto));
    }

    @PreAuthorize("hasPermission('masterdata:update')")
    @PutMapping("/mappings/{mappingId}")
    public R<DataElementMappingEntity> updateMapping(@PathVariable Long mappingId,
                                                      @RequestParam String mappingStatus) {
        return R.ok(service.updateMapping(mappingId, mappingStatus));
    }

    @PreAuthorize("hasPermission('masterdata:delete')")
    @DeleteMapping("/mappings/{mappingId}")
    public R<Void> deleteMapping(@PathVariable Long mappingId) {
        service.deleteMapping(mappingId);
        return R.ok();
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/mappings/unmapped")
    public R<List<DataElementMappingEntity>> getUnmapped() {
        return R.ok(service.getUnmapped());
    }

    // ── 分类与统计 ──

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/categories")
    public R<List<String>> getCategories() {
        return R.ok(service.getCategories());
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        return R.ok(service.getStats());
    }

    // ── 导入 ──

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public R<ImportTaskEntity> importExcel(@RequestParam("file") MultipartFile file) {
        return R.ok(importService.uploadAndStart(file));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/import/tasks/{taskId}")
    public R<ImportTaskEntity> getImportTaskStatus(@PathVariable Long taskId) {
        return R.ok(importService.getTaskStatus(taskId));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] data = importService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data-element-template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
