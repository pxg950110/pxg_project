package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.ImportTaskEntity;
import com.maidc.data.service.MasterDataImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/masterdata/import")
@RequiredArgsConstructor
public class MasterDataImportController {

    private final MasterDataImportService importService;

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public R<ImportTaskEntity> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("code_system_id") Long codeSystemId) {
        return R.ok(importService.uploadAndCreateTask(file, codeSystemId));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/tasks/{taskId}")
    public R<ImportTaskEntity> getTaskStatus(@PathVariable Long taskId) {
        return R.ok(importService.getTaskStatus(taskId));
    }
}
