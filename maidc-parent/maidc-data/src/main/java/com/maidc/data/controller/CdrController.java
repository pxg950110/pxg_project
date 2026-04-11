package com.maidc.data.controller;

import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.data.dto.PatientCreateDTO;
import com.maidc.data.dto.PatientQueryDTO;
import com.maidc.data.entity.*;
import com.maidc.data.service.*;
import com.maidc.data.vo.EncounterVO;
import com.maidc.data.vo.PatientDetailVO;
import com.maidc.data.vo.PatientVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cdr")
@RequiredArgsConstructor
public class CdrController {

    private final PatientService patientService;
    private final EncounterService encounterService;
    private final Patient360Service patient360Service;
    private final LabTestService labTestService;
    private final LabPanelService labPanelService;
    private final MedicationService medicationService;
    private final VitalSignService vitalSignService;
    private final ImagingExamService imagingExamService;
    private final ImagingFindingService imagingFindingService;
    private final PathologyService pathologyService;
    private final OperationService operationService;
    private final AllergyService allergyService;

    // ==================== Patient ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/patients")
    public R<PageResult<PatientVO>> listPatients(PatientQueryDTO query) {
        return R.ok(patientService.listPatients(query));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/patients/{id}")
    public R<PatientVO> getPatient(@PathVariable Long id) {
        return R.ok(patientService.getPatient(id));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/patients/{id}/360")
    public R<PatientDetailVO> getPatient360(@PathVariable Long id) {
        return R.ok(patient360Service.getPatient360(id));
    }

    @OperLog(module = "cdr", operation = "createPatient")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/patients")
    public R<PatientVO> createPatient(@RequestBody @Valid PatientCreateDTO dto) {
        return R.ok(patientService.createPatient(dto));
    }

    @OperLog(module = "cdr", operation = "deletePatient")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/patients/{id}")
    public R<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return R.ok();
    }

    // ==================== Encounter ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/patients/{patientId}/encounters")
    public R<List<EncounterVO>> listEncounters(@PathVariable Long patientId) {
        return R.ok(encounterService.findByPatientId(patientId));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/encounters/{id}")
    public R<EncounterVO> getEncounter(@PathVariable Long id) {
        return R.ok(encounterService.getEncounter(id));
    }

    // ==================== LabTest ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/lab-tests")
    public R<Page<LabTestEntity>> listLabTests(
            @RequestParam(required = false) Long encounterId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(labTestService.listLabTests(encounterId, page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/lab-tests/{id}")
    public R<LabTestEntity> getLabTest(@PathVariable Long id) {
        return R.ok(labTestService.getLabTest(id));
    }

    @OperLog(module = "cdr", operation = "createLabTest")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/lab-tests")
    public R<LabTestEntity> createLabTest(@RequestBody LabTestEntity entity) {
        return R.ok(labTestService.createLabTest(entity));
    }

    @OperLog(module = "cdr", operation = "deleteLabTest")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/lab-tests/{id}")
    public R<Void> deleteLabTest(@PathVariable Long id) {
        labTestService.deleteLabTest(id);
        return R.ok();
    }

    // ==================== LabPanel ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/lab-panels")
    public R<Page<LabPanelEntity>> listLabPanels(
            @RequestParam(required = false) Long testId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(labPanelService.listLabPanels(testId, page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/lab-panels/{id}")
    public R<LabPanelEntity> getLabPanel(@PathVariable Long id) {
        return R.ok(labPanelService.getLabPanel(id));
    }

    @OperLog(module = "cdr", operation = "createLabPanel")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/lab-panels")
    public R<LabPanelEntity> createLabPanel(@RequestBody LabPanelEntity entity) {
        return R.ok(labPanelService.createLabPanel(entity));
    }

    @OperLog(module = "cdr", operation = "deleteLabPanel")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/lab-panels/{id}")
    public R<Void> deleteLabPanel(@PathVariable Long id) {
        labPanelService.deleteLabPanel(id);
        return R.ok();
    }

    // ==================== Medication ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/medications")
    public R<Page<MedicationEntity>> listMedications(
            @RequestParam(required = false) Long encounterId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(medicationService.listMedications(encounterId, page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/medications/{id}")
    public R<MedicationEntity> getMedication(@PathVariable Long id) {
        return R.ok(medicationService.getMedication(id));
    }

    @OperLog(module = "cdr", operation = "createMedication")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/medications")
    public R<MedicationEntity> createMedication(@RequestBody MedicationEntity entity) {
        return R.ok(medicationService.createMedication(entity));
    }

    @OperLog(module = "cdr", operation = "deleteMedication")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/medications/{id}")
    public R<Void> deleteMedication(@PathVariable Long id) {
        medicationService.deleteMedication(id);
        return R.ok();
    }

    // ==================== VitalSign ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/vital-signs")
    public R<Page<VitalSignEntity>> listVitalSigns(
            @RequestParam(required = false) Long encounterId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(vitalSignService.listVitalSigns(encounterId, page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/vital-signs/{id}")
    public R<VitalSignEntity> getVitalSign(@PathVariable Long id) {
        return R.ok(vitalSignService.getVitalSign(id));
    }

    @OperLog(module = "cdr", operation = "createVitalSign")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/vital-signs")
    public R<VitalSignEntity> createVitalSign(@RequestBody VitalSignEntity entity) {
        return R.ok(vitalSignService.createVitalSign(entity));
    }

    @OperLog(module = "cdr", operation = "deleteVitalSign")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/vital-signs/{id}")
    public R<Void> deleteVitalSign(@PathVariable Long id) {
        vitalSignService.deleteVitalSign(id);
        return R.ok();
    }

    // ==================== ImagingExam ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/imaging-exams")
    public R<Page<ImagingExamEntity>> listImagingExams(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(imagingExamService.listImagingExams(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/imaging-exams/{id}")
    public R<ImagingExamEntity> getImagingExam(@PathVariable Long id) {
        return R.ok(imagingExamService.getImagingExam(id));
    }

    @OperLog(module = "cdr", operation = "createImagingExam")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/imaging-exams")
    public R<ImagingExamEntity> createImagingExam(@RequestBody ImagingExamEntity entity) {
        return R.ok(imagingExamService.createImagingExam(entity));
    }

    @OperLog(module = "cdr", operation = "deleteImagingExam")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/imaging-exams/{id}")
    public R<Void> deleteImagingExam(@PathVariable Long id) {
        imagingExamService.deleteImagingExam(id);
        return R.ok();
    }

    // ==================== ImagingFinding ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/imaging-findings")
    public R<Page<ImagingFindingEntity>> listImagingFindings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(imagingFindingService.listImagingFindings(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/imaging-findings/{id}")
    public R<ImagingFindingEntity> getImagingFinding(@PathVariable Long id) {
        return R.ok(imagingFindingService.getImagingFinding(id));
    }

    @OperLog(module = "cdr", operation = "createImagingFinding")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/imaging-findings")
    public R<ImagingFindingEntity> createImagingFinding(@RequestBody ImagingFindingEntity entity) {
        return R.ok(imagingFindingService.createImagingFinding(entity));
    }

    @OperLog(module = "cdr", operation = "deleteImagingFinding")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/imaging-findings/{id}")
    public R<Void> deleteImagingFinding(@PathVariable Long id) {
        imagingFindingService.deleteImagingFinding(id);
        return R.ok();
    }

    // ==================== Pathology ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/pathologies")
    public R<Page<PathologyEntity>> listPathologies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(pathologyService.listPathologies(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/pathologies/{id}")
    public R<PathologyEntity> getPathology(@PathVariable Long id) {
        return R.ok(pathologyService.getPathology(id));
    }

    @OperLog(module = "cdr", operation = "createPathology")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/pathologies")
    public R<PathologyEntity> createPathology(@RequestBody PathologyEntity entity) {
        return R.ok(pathologyService.createPathology(entity));
    }

    @OperLog(module = "cdr", operation = "deletePathology")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/pathologies/{id}")
    public R<Void> deletePathology(@PathVariable Long id) {
        pathologyService.deletePathology(id);
        return R.ok();
    }

    // ==================== Operation ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/operations")
    public R<Page<OperationEntity>> listOperations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(operationService.listOperations(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/operations/{id}")
    public R<OperationEntity> getOperation(@PathVariable Long id) {
        return R.ok(operationService.getOperation(id));
    }

    @OperLog(module = "cdr", operation = "createOperation")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/operations")
    public R<OperationEntity> createOperation(@RequestBody OperationEntity entity) {
        return R.ok(operationService.createOperation(entity));
    }

    @OperLog(module = "cdr", operation = "deleteOperation")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/operations/{id}")
    public R<Void> deleteOperation(@PathVariable Long id) {
        operationService.deleteOperation(id);
        return R.ok();
    }

    // ==================== Allergy ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/allergies")
    public R<Page<AllergyEntity>> listAllergies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(allergyService.listAllergies(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/allergies/{id}")
    public R<AllergyEntity> getAllergy(@PathVariable Long id) {
        return R.ok(allergyService.getAllergy(id));
    }

    @OperLog(module = "cdr", operation = "createAllergy")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/allergies")
    public R<AllergyEntity> createAllergy(@RequestBody AllergyEntity entity) {
        return R.ok(allergyService.createAllergy(entity));
    }

    @OperLog(module = "cdr", operation = "deleteAllergy")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/allergies/{id}")
    public R<Void> deleteAllergy(@PathVariable Long id) {
        allergyService.deleteAllergy(id);
        return R.ok();
    }
}
