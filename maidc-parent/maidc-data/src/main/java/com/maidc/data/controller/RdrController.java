package com.maidc.data.controller;

import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.data.dto.DatasetCreateDTO;
import com.maidc.data.dto.DatasetQueryDTO;
import com.maidc.data.dto.ProjectCreateDTO;
import com.maidc.data.dto.ProjectQueryDTO;
import com.maidc.data.entity.ResearchCohortEntity;
import com.maidc.data.entity.StudySubjectEntity;
import com.maidc.data.service.DatasetService;
import com.maidc.data.service.ProjectService;
import com.maidc.data.service.ResearchCohortService;
import com.maidc.data.service.StudySubjectService;
import com.maidc.data.vo.DatasetDetailVO;
import com.maidc.data.vo.DatasetVO;
import com.maidc.data.vo.ProjectDetailVO;
import com.maidc.data.vo.ProjectVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rdr")
@RequiredArgsConstructor
public class RdrController {

    private final ProjectService projectService;
    private final DatasetService datasetService;
    private final ResearchCohortService researchCohortService;
    private final StudySubjectService studySubjectService;

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

    // ==================== Research Cohort ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/research-cohorts")
    public R<Page<ResearchCohortEntity>> listCohorts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(researchCohortService.listCohorts(page, size));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/research-cohorts/{id}")
    public R<ResearchCohortEntity> getCohort(@PathVariable Long id) {
        return R.ok(researchCohortService.getCohort(id));
    }

    @OperLog(module = "rdr", operation = "createCohort")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/research-cohorts")
    public R<ResearchCohortEntity> createCohort(@RequestBody ResearchCohortEntity entity) {
        return R.ok(researchCohortService.createCohort(entity));
    }

    @OperLog(module = "rdr", operation = "deleteCohort")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/research-cohorts/{id}")
    public R<Void> deleteCohort(@PathVariable Long id) {
        researchCohortService.deleteCohort(id);
        return R.ok();
    }

    // ==================== Study Subject ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/study-subjects")
    public R<Page<StudySubjectEntity>> listSubjects(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(studySubjectService.listSubjects(page, size));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/study-subjects/{id}")
    public R<StudySubjectEntity> getSubject(@PathVariable Long id) {
        return R.ok(studySubjectService.getSubject(id));
    }

    @OperLog(module = "rdr", operation = "createSubject")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/study-subjects")
    public R<StudySubjectEntity> createSubject(@RequestBody StudySubjectEntity entity) {
        return R.ok(studySubjectService.createSubject(entity));
    }

    @OperLog(module = "rdr", operation = "deleteSubject")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/study-subjects/{id}")
    public R<Void> deleteSubject(@PathVariable Long id) {
        studySubjectService.deleteSubject(id);
        return R.ok();
    }
}
