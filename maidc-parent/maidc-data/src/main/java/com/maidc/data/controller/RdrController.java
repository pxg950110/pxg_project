package com.maidc.data.controller;

import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.data.dto.DatasetCreateDTO;
import com.maidc.data.dto.DatasetQueryDTO;
import com.maidc.data.dto.ProjectCreateDTO;
import com.maidc.data.dto.ProjectQueryDTO;
import com.maidc.data.service.DatasetService;
import com.maidc.data.service.ProjectService;
import com.maidc.data.vo.DatasetDetailVO;
import com.maidc.data.vo.DatasetVO;
import com.maidc.data.vo.ProjectDetailVO;
import com.maidc.data.vo.ProjectVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rdr")
@RequiredArgsConstructor
public class RdrController {

    private final ProjectService projectService;
    private final DatasetService datasetService;

    // ==================== Project ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/projects")
    public R<PageResult<ProjectVO>> listProjects(ProjectQueryDTO query) {
        return R.ok(projectService.listProjects(query));
    }

    @OperLog(module = "rdr", operation = "createProject")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/projects")
    public R<ProjectVO> createProject(@RequestBody @Valid ProjectCreateDTO dto) {
        return R.ok(projectService.createProject(dto));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/projects/{id}")
    public R<ProjectDetailVO> getProject(@PathVariable Long id) {
        return R.ok(projectService.getProjectDetail(id));
    }

    @OperLog(module = "rdr", operation = "addMember")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/projects/{id}/members")
    public R<Void> addMember(@PathVariable Long id,
                             @RequestParam Long userId,
                             @RequestParam String role) {
        projectService.addMember(id, userId, role);
        return R.ok();
    }

    @OperLog(module = "rdr", operation = "removeMember")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/projects/{id}/members/{userId}")
    public R<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        projectService.removeMember(id, userId);
        return R.ok();
    }

    @OperLog(module = "rdr", operation = "deleteProject")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/projects/{id}")
    public R<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return R.ok();
    }

    // ==================== Dataset ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/datasets")
    public R<PageResult<DatasetVO>> listDatasets(DatasetQueryDTO query) {
        return R.ok(datasetService.listDatasets(query));
    }

    @OperLog(module = "rdr", operation = "createDataset")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/datasets")
    public R<DatasetVO> createDataset(@RequestBody @Valid DatasetCreateDTO dto) {
        return R.ok(datasetService.createDataset(dto));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/datasets/{id}")
    public R<DatasetDetailVO> getDataset(@PathVariable Long id) {
        return R.ok(datasetService.getDatasetDetail(id));
    }

    @OperLog(module = "rdr", operation = "deleteDataset")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/datasets/{id}")
    public R<Void> deleteDataset(@PathVariable Long id) {
        datasetService.deleteDataset(id);
        return R.ok();
    }
}
