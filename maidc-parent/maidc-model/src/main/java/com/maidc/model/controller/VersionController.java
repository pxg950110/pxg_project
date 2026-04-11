package com.maidc.model.controller;

import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.model.service.VersionService;
import com.maidc.model.vo.VersionCompareVO;
import com.maidc.model.vo.VersionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/models/{modelId}/versions")
@RequiredArgsConstructor
public class VersionController {

    private final VersionService versionService;

    @OperLog(module = "model", operation = "uploadVersion")
    @PreAuthorize("hasPermission('model:create')")
    @PostMapping(consumes = "multipart/form-data")
    public R<VersionVO> createVersion(
            @PathVariable Long modelId,
            @RequestParam String version_no,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String changelog,
            @RequestParam(required = false) String hyper_params,
            @RequestParam("model_file") MultipartFile modelFile,
            @RequestParam(value = "config_file", required = false) MultipartFile configFile) {
        return R.ok(versionService.createVersion(modelId, version_no, description, changelog,
                hyper_params, modelFile, configFile));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping
    public R<PageResult<VersionVO>> listVersions(
            @PathVariable Long modelId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(versionService.listVersions(modelId, page, pageSize));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/{versionId}")
    public R<VersionVO> getVersion(@PathVariable Long modelId, @PathVariable Long versionId) {
        return R.ok(versionService.getVersionDetail(modelId, versionId));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/{versionId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadVersion(
            @PathVariable Long modelId, @PathVariable Long versionId) {
        return versionService.downloadVersion(modelId, versionId);
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/compare")
    public R<VersionCompareVO> compareVersions(
            @PathVariable Long modelId,
            @RequestParam Long v1, @RequestParam Long v2) {
        return R.ok(versionService.compareVersions(modelId, v1, v2));
    }
}
