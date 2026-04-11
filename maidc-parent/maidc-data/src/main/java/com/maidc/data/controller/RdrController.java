package com.maidc.data.controller;

import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.data.dto.DatasetCreateDTO;
import com.maidc.data.dto.DatasetQueryDTO;
import com.maidc.data.dto.ProjectCreateDTO;
import com.maidc.data.dto.ProjectQueryDTO;
import com.maidc.data.entity.ClinicalFeatureEntity;
import com.maidc.data.entity.DataQualityResultEntity;
import com.maidc.data.entity.DataQualityRuleEntity;
import com.maidc.data.entity.DatasetAccessLogEntity;
import com.maidc.data.entity.EtlTaskLogEntity;
import com.maidc.data.entity.FeatureDictionaryEntity;
import com.maidc.data.entity.GenomicDatasetEntity;
import com.maidc.data.entity.GenomicVariantEntity;
import com.maidc.data.entity.ImagingAnnotationEntity;
import com.maidc.data.entity.ImagingDatasetEntity;
import com.maidc.data.entity.ResearchCohortEntity;
import com.maidc.data.entity.StudySubjectEntity;
import com.maidc.data.entity.TextAnnotationEntity;
import com.maidc.data.entity.TextDatasetEntity;
import com.maidc.data.service.ClinicalFeatureService;
import com.maidc.data.service.DataQualityResultService;
import com.maidc.data.service.DataQualityRuleService;
import com.maidc.data.service.DatasetAccessLogService;
import com.maidc.data.service.DatasetService;
import com.maidc.data.service.EtlTaskLogService;
import com.maidc.data.service.FeatureDictionaryService;
import com.maidc.data.service.GenomicDatasetService;
import com.maidc.data.service.GenomicVariantService;
import com.maidc.data.service.ImagingAnnotationService;
import com.maidc.data.service.ImagingDatasetService;
import com.maidc.data.service.ProjectService;
import com.maidc.data.service.ResearchCohortService;
import com.maidc.data.service.StudySubjectService;
import com.maidc.data.service.TextAnnotationService;
import com.maidc.data.service.TextDatasetService;
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
    private final DatasetAccessLogService datasetAccessLogService;
    private final ClinicalFeatureService clinicalFeatureService;
    private final FeatureDictionaryService featureDictionaryService;
    private final ImagingDatasetService imagingDatasetService;
    private final ImagingAnnotationService imagingAnnotationService;
    private final GenomicDatasetService genomicDatasetService;
    private final GenomicVariantService genomicVariantService;
    private final TextDatasetService textDatasetService;
    private final TextAnnotationService textAnnotationService;
    private final EtlTaskLogService etlTaskLogService;
    private final DataQualityRuleService dataQualityRuleService;
    private final DataQualityResultService dataQualityResultService;

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

    // ==================== Dataset Access Log ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/dataset-access-logs")
    public R<Page<DatasetAccessLogEntity>> listAccessLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(datasetAccessLogService.listAccessLogs(page, size));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/dataset-access-logs/{id}")
    public R<DatasetAccessLogEntity> getAccessLog(@PathVariable Long id) {
        return R.ok(datasetAccessLogService.getAccessLog(id));
    }

    @OperLog(module = "rdr", operation = "createAccessLog")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/dataset-access-logs")
    public R<DatasetAccessLogEntity> createAccessLog(@RequestBody DatasetAccessLogEntity entity) {
        return R.ok(datasetAccessLogService.createAccessLog(entity));
    }

    @OperLog(module = "rdr", operation = "deleteAccessLog")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/dataset-access-logs/{id}")
    public R<Void> deleteAccessLog(@PathVariable Long id) {
        datasetAccessLogService.deleteAccessLog(id);
        return R.ok();
    }

    // ==================== Clinical Feature ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/clinical-features")
    public R<Page<ClinicalFeatureEntity>> listFeatures(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(clinicalFeatureService.listFeatures(page, size));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/clinical-features/{id}")
    public R<ClinicalFeatureEntity> getFeature(@PathVariable Long id) {
        return R.ok(clinicalFeatureService.getFeature(id));
    }

    @OperLog(module = "rdr", operation = "createFeature")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/clinical-features")
    public R<ClinicalFeatureEntity> createFeature(@RequestBody ClinicalFeatureEntity entity) {
        return R.ok(clinicalFeatureService.createFeature(entity));
    }

    @OperLog(module = "rdr", operation = "deleteFeature")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/clinical-features/{id}")
    public R<Void> deleteFeature(@PathVariable Long id) {
        clinicalFeatureService.deleteFeature(id);
        return R.ok();
    }

    // ==================== Feature Dictionary ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/feature-dictionary")
    public R<Page<FeatureDictionaryEntity>> listDictionaries(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(featureDictionaryService.listDictionaries(page, size));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/feature-dictionary/{id}")
    public R<FeatureDictionaryEntity> getDictionary(@PathVariable Long id) {
        return R.ok(featureDictionaryService.getDictionary(id));
    }

    @OperLog(module = "rdr", operation = "createDictionary")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/feature-dictionary")
    public R<FeatureDictionaryEntity> createDictionary(@RequestBody FeatureDictionaryEntity entity) {
        return R.ok(featureDictionaryService.createDictionary(entity));
    }

    @OperLog(module = "rdr", operation = "deleteDictionary")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/feature-dictionary/{id}")
    public R<Void> deleteDictionary(@PathVariable Long id) {
        featureDictionaryService.deleteDictionary(id);
        return R.ok();
    }

    // ==================== Imaging Dataset ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/imaging-datasets")
    public R<Page<ImagingDatasetEntity>> listImagingDatasets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(imagingDatasetService.listImagingDatasets(page, size));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/imaging-datasets/{id}")
    public R<ImagingDatasetEntity> getImagingDataset(@PathVariable Long id) {
        return R.ok(imagingDatasetService.getImagingDataset(id));
    }

    @OperLog(module = "rdr", operation = "createImagingDataset")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/imaging-datasets")
    public R<ImagingDatasetEntity> createImagingDataset(@RequestBody ImagingDatasetEntity entity) {
        return R.ok(imagingDatasetService.createImagingDataset(entity));
    }

    @OperLog(module = "rdr", operation = "deleteImagingDataset")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/imaging-datasets/{id}")
    public R<Void> deleteImagingDataset(@PathVariable Long id) {
        imagingDatasetService.deleteImagingDataset(id);
        return R.ok();
    }

    // ==================== Imaging Annotation ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/imaging-annotations")
    public R<Page<ImagingAnnotationEntity>> listAnnotations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(imagingAnnotationService.listAnnotations(page, size));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/imaging-annotations/{id}")
    public R<ImagingAnnotationEntity> getAnnotation(@PathVariable Long id) {
        return R.ok(imagingAnnotationService.getAnnotation(id));
    }

    @OperLog(module = "rdr", operation = "createAnnotation")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/imaging-annotations")
    public R<ImagingAnnotationEntity> createAnnotation(@RequestBody ImagingAnnotationEntity entity) {
        return R.ok(imagingAnnotationService.createAnnotation(entity));
    }

    @OperLog(module = "rdr", operation = "deleteAnnotation")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/imaging-annotations/{id}")
    public R<Void> deleteAnnotation(@PathVariable Long id) {
        imagingAnnotationService.deleteAnnotation(id);
        return R.ok();
    }

    // ==================== Genomic Dataset ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/genomic-datasets")
    public R<Page<GenomicDatasetEntity>> listGenomicDatasets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(genomicDatasetService.listGenomicDatasets(page, size));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/genomic-datasets/{id}")
    public R<GenomicDatasetEntity> getGenomicDataset(@PathVariable Long id) {
        return R.ok(genomicDatasetService.getGenomicDataset(id));
    }

    @OperLog(module = "rdr", operation = "createGenomicDataset")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/genomic-datasets")
    public R<GenomicDatasetEntity> createGenomicDataset(@RequestBody GenomicDatasetEntity entity) {
        return R.ok(genomicDatasetService.createGenomicDataset(entity));
    }

    @OperLog(module = "rdr", operation = "deleteGenomicDataset")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/genomic-datasets/{id}")
    public R<Void> deleteGenomicDataset(@PathVariable Long id) {
        genomicDatasetService.deleteGenomicDataset(id);
        return R.ok();
    }

    // ==================== Genomic Variant ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/genomic-variants")
    public R<Page<GenomicVariantEntity>> listGenomicVariants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(genomicVariantService.listGenomicVariants(page, size));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/genomic-variants/{id}")
    public R<GenomicVariantEntity> getGenomicVariant(@PathVariable Long id) {
        return R.ok(genomicVariantService.getGenomicVariant(id));
    }

    @OperLog(module = "rdr", operation = "createGenomicVariant")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/genomic-variants")
    public R<GenomicVariantEntity> createGenomicVariant(@RequestBody GenomicVariantEntity entity) {
        return R.ok(genomicVariantService.createGenomicVariant(entity));
    }

    @OperLog(module = "rdr", operation = "deleteGenomicVariant")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/genomic-variants/{id}")
    public R<Void> deleteGenomicVariant(@PathVariable Long id) {
        genomicVariantService.deleteGenomicVariant(id);
        return R.ok();
    }

    // ==================== Text Dataset ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/text-datasets")
    public R<Page<TextDatasetEntity>> listTextDatasets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(textDatasetService.listTextDatasets(page, size));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/text-datasets/{id}")
    public R<TextDatasetEntity> getTextDataset(@PathVariable Long id) {
        return R.ok(textDatasetService.getTextDataset(id));
    }

    @OperLog(module = "rdr", operation = "createTextDataset")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/text-datasets")
    public R<TextDatasetEntity> createTextDataset(@RequestBody TextDatasetEntity entity) {
        return R.ok(textDatasetService.createTextDataset(entity));
    }

    @OperLog(module = "rdr", operation = "deleteTextDataset")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/text-datasets/{id}")
    public R<Void> deleteTextDataset(@PathVariable Long id) {
        textDatasetService.deleteTextDataset(id);
        return R.ok();
    }

    // ==================== Text Annotation ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/text-annotations")
    public R<Page<TextAnnotationEntity>> listTextAnnotations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(textAnnotationService.listTextAnnotations(page, size));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/text-annotations/{id}")
    public R<TextAnnotationEntity> getTextAnnotation(@PathVariable Long id) {
        return R.ok(textAnnotationService.getTextAnnotation(id));
    }

    @OperLog(module = "rdr", operation = "createTextAnnotation")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/text-annotations")
    public R<TextAnnotationEntity> createTextAnnotation(@RequestBody TextAnnotationEntity entity) {
        return R.ok(textAnnotationService.createTextAnnotation(entity));
    }

    @OperLog(module = "rdr", operation = "deleteTextAnnotation")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/text-annotations/{id}")
    public R<Void> deleteTextAnnotation(@PathVariable Long id) {
        textAnnotationService.deleteTextAnnotation(id);
        return R.ok();
    }

    // ==================== ETL Task Log ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/etl-task-logs")
    public R<Page<EtlTaskLogEntity>> listEtlTaskLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(etlTaskLogService.listEtlTaskLogs(page, size));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/etl-task-logs/{id}")
    public R<EtlTaskLogEntity> getEtlTaskLog(@PathVariable Long id) {
        return R.ok(etlTaskLogService.getEtlTaskLog(id));
    }

    @OperLog(module = "rdr", operation = "createEtlTaskLog")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/etl-task-logs")
    public R<EtlTaskLogEntity> createEtlTaskLog(@RequestBody EtlTaskLogEntity entity) {
        return R.ok(etlTaskLogService.createEtlTaskLog(entity));
    }

    @OperLog(module = "rdr", operation = "deleteEtlTaskLog")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/etl-task-logs/{id}")
    public R<Void> deleteEtlTaskLog(@PathVariable Long id) {
        etlTaskLogService.deleteEtlTaskLog(id);
        return R.ok();
    }

    // ==================== Data Quality Rule ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/quality-rules")
    public R<Page<DataQualityRuleEntity>> listQualityRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(dataQualityRuleService.listDataQualityRules(page, size));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/quality-rules/{id}")
    public R<DataQualityRuleEntity> getQualityRule(@PathVariable Long id) {
        return R.ok(dataQualityRuleService.getDataQualityRule(id));
    }

    @OperLog(module = "rdr", operation = "createQualityRule")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/quality-rules")
    public R<DataQualityRuleEntity> createQualityRule(@RequestBody DataQualityRuleEntity entity) {
        return R.ok(dataQualityRuleService.createDataQualityRule(entity));
    }

    @OperLog(module = "rdr", operation = "deleteQualityRule")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/quality-rules/{id}")
    public R<Void> deleteQualityRule(@PathVariable Long id) {
        dataQualityRuleService.deleteDataQualityRule(id);
        return R.ok();
    }

    // ==================== Data Quality Result ====================

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/quality-results")
    public R<Page<DataQualityResultEntity>> listQualityResults(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(dataQualityResultService.listDataQualityResults(page, size));
    }

    @PreAuthorize("hasPermission('rdr:read')")
    @GetMapping("/quality-results/{id}")
    public R<DataQualityResultEntity> getQualityResult(@PathVariable Long id) {
        return R.ok(dataQualityResultService.getDataQualityResult(id));
    }

    @OperLog(module = "rdr", operation = "createQualityResult")
    @PreAuthorize("hasPermission('rdr:create')")
    @PostMapping("/quality-results")
    public R<DataQualityResultEntity> createQualityResult(@RequestBody DataQualityResultEntity entity) {
        return R.ok(dataQualityResultService.createDataQualityResult(entity));
    }

    @OperLog(module = "rdr", operation = "deleteQualityResult")
    @PreAuthorize("hasPermission('rdr:create')")
    @DeleteMapping("/quality-results/{id}")
    public R<Void> deleteQualityResult(@PathVariable Long id) {
        dataQualityResultService.deleteDataQualityResult(id);
        return R.ok();
    }
}
