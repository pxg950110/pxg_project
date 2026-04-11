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
    private final FamilyHistoryService familyHistoryService;
    private final ClinicalNoteService clinicalNoteService;
    private final DischargeSummaryService dischargeSummaryService;
    private final PatientContactService patientContactService;
    private final PatientInsuranceService patientInsuranceService;
    private final PatientBedService patientBedService;
    private final NursingRecordService nursingRecordService;
    private final BloodTransfusionService bloodTransfusionService;
    private final TransferService transferService;
    private final FeeRecordService feeRecordService;

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

    // ==================== FamilyHistory ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/family-histories")
    public R<Page<FamilyHistoryEntity>> listFamilyHistories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(familyHistoryService.listFamilyHistories(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/family-histories/{id}")
    public R<FamilyHistoryEntity> getFamilyHistory(@PathVariable Long id) {
        return R.ok(familyHistoryService.getFamilyHistory(id));
    }

    @OperLog(module = "cdr", operation = "createFamilyHistory")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/family-histories")
    public R<FamilyHistoryEntity> createFamilyHistory(@RequestBody FamilyHistoryEntity entity) {
        return R.ok(familyHistoryService.createFamilyHistory(entity));
    }

    @OperLog(module = "cdr", operation = "deleteFamilyHistory")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/family-histories/{id}")
    public R<Void> deleteFamilyHistory(@PathVariable Long id) {
        familyHistoryService.deleteFamilyHistory(id);
        return R.ok();
    }

    // ==================== ClinicalNote ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/clinical-notes")
    public R<Page<ClinicalNoteEntity>> listClinicalNotes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(clinicalNoteService.listClinicalNotes(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/clinical-notes/{id}")
    public R<ClinicalNoteEntity> getClinicalNote(@PathVariable Long id) {
        return R.ok(clinicalNoteService.getClinicalNote(id));
    }

    @OperLog(module = "cdr", operation = "createClinicalNote")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/clinical-notes")
    public R<ClinicalNoteEntity> createClinicalNote(@RequestBody ClinicalNoteEntity entity) {
        return R.ok(clinicalNoteService.createClinicalNote(entity));
    }

    @OperLog(module = "cdr", operation = "deleteClinicalNote")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/clinical-notes/{id}")
    public R<Void> deleteClinicalNote(@PathVariable Long id) {
        clinicalNoteService.deleteClinicalNote(id);
        return R.ok();
    }

    // ==================== DischargeSummary ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/discharge-summaries")
    public R<Page<DischargeSummaryEntity>> listDischargeSummaries(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(dischargeSummaryService.listDischargeSummaries(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/discharge-summaries/{id}")
    public R<DischargeSummaryEntity> getDischargeSummary(@PathVariable Long id) {
        return R.ok(dischargeSummaryService.getDischargeSummary(id));
    }

    @OperLog(module = "cdr", operation = "createDischargeSummary")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/discharge-summaries")
    public R<DischargeSummaryEntity> createDischargeSummary(@RequestBody DischargeSummaryEntity entity) {
        return R.ok(dischargeSummaryService.createDischargeSummary(entity));
    }

    @OperLog(module = "cdr", operation = "deleteDischargeSummary")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/discharge-summaries/{id}")
    public R<Void> deleteDischargeSummary(@PathVariable Long id) {
        dischargeSummaryService.deleteDischargeSummary(id);
        return R.ok();
    }

    // ==================== PatientContact ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/patient-contacts")
    public R<Page<PatientContactEntity>> listPatientContacts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(patientContactService.listPatientContacts(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/patient-contacts/{id}")
    public R<PatientContactEntity> getPatientContact(@PathVariable Long id) {
        return R.ok(patientContactService.getPatientContact(id));
    }

    @OperLog(module = "cdr", operation = "createPatientContact")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/patient-contacts")
    public R<PatientContactEntity> createPatientContact(@RequestBody PatientContactEntity entity) {
        return R.ok(patientContactService.createPatientContact(entity));
    }

    @OperLog(module = "cdr", operation = "deletePatientContact")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/patient-contacts/{id}")
    public R<Void> deletePatientContact(@PathVariable Long id) {
        patientContactService.deletePatientContact(id);
        return R.ok();
    }

    // ==================== PatientInsurance ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/patient-insurances")
    public R<Page<PatientInsuranceEntity>> listPatientInsurances(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(patientInsuranceService.listPatientInsurances(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/patient-insurances/{id}")
    public R<PatientInsuranceEntity> getPatientInsurance(@PathVariable Long id) {
        return R.ok(patientInsuranceService.getPatientInsurance(id));
    }

    @OperLog(module = "cdr", operation = "createPatientInsurance")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/patient-insurances")
    public R<PatientInsuranceEntity> createPatientInsurance(@RequestBody PatientInsuranceEntity entity) {
        return R.ok(patientInsuranceService.createPatientInsurance(entity));
    }

    @OperLog(module = "cdr", operation = "deletePatientInsurance")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/patient-insurances/{id}")
    public R<Void> deletePatientInsurance(@PathVariable Long id) {
        patientInsuranceService.deletePatientInsurance(id);
        return R.ok();
    }

    // ==================== PatientBed ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/patient-beds")
    public R<Page<PatientBedEntity>> listPatientBeds(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(patientBedService.listPatientBeds(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/patient-beds/{id}")
    public R<PatientBedEntity> getPatientBed(@PathVariable Long id) {
        return R.ok(patientBedService.getPatientBed(id));
    }

    @OperLog(module = "cdr", operation = "createPatientBed")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/patient-beds")
    public R<PatientBedEntity> createPatientBed(@RequestBody PatientBedEntity entity) {
        return R.ok(patientBedService.createPatientBed(entity));
    }

    @OperLog(module = "cdr", operation = "deletePatientBed")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/patient-beds/{id}")
    public R<Void> deletePatientBed(@PathVariable Long id) {
        patientBedService.deletePatientBed(id);
        return R.ok();
    }

    // ==================== NursingRecord ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/nursing-records")
    public R<Page<NursingRecordEntity>> listNursingRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(nursingRecordService.listNursingRecords(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/nursing-records/{id}")
    public R<NursingRecordEntity> getNursingRecord(@PathVariable Long id) {
        return R.ok(nursingRecordService.getNursingRecord(id));
    }

    @OperLog(module = "cdr", operation = "createNursingRecord")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/nursing-records")
    public R<NursingRecordEntity> createNursingRecord(@RequestBody NursingRecordEntity entity) {
        return R.ok(nursingRecordService.createNursingRecord(entity));
    }

    @OperLog(module = "cdr", operation = "deleteNursingRecord")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/nursing-records/{id}")
    public R<Void> deleteNursingRecord(@PathVariable Long id) {
        nursingRecordService.deleteNursingRecord(id);
        return R.ok();
    }

    // ==================== BloodTransfusion ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/blood-transfusions")
    public R<Page<BloodTransfusionEntity>> listBloodTransfusions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(bloodTransfusionService.listBloodTransfusions(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/blood-transfusions/{id}")
    public R<BloodTransfusionEntity> getBloodTransfusion(@PathVariable Long id) {
        return R.ok(bloodTransfusionService.getBloodTransfusion(id));
    }

    @OperLog(module = "cdr", operation = "createBloodTransfusion")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/blood-transfusions")
    public R<BloodTransfusionEntity> createBloodTransfusion(@RequestBody BloodTransfusionEntity entity) {
        return R.ok(bloodTransfusionService.createBloodTransfusion(entity));
    }

    @OperLog(module = "cdr", operation = "deleteBloodTransfusion")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/blood-transfusions/{id}")
    public R<Void> deleteBloodTransfusion(@PathVariable Long id) {
        bloodTransfusionService.deleteBloodTransfusion(id);
        return R.ok();
    }

    // ==================== Transfer ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/transfers")
    public R<Page<TransferEntity>> listTransfers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(transferService.listTransfers(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/transfers/{id}")
    public R<TransferEntity> getTransfer(@PathVariable Long id) {
        return R.ok(transferService.getTransfer(id));
    }

    @OperLog(module = "cdr", operation = "createTransfer")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/transfers")
    public R<TransferEntity> createTransfer(@RequestBody TransferEntity entity) {
        return R.ok(transferService.createTransfer(entity));
    }

    @OperLog(module = "cdr", operation = "deleteTransfer")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/transfers/{id}")
    public R<Void> deleteTransfer(@PathVariable Long id) {
        transferService.deleteTransfer(id);
        return R.ok();
    }

    // ==================== FeeRecord ====================

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/fee-records")
    public R<Page<FeeRecordEntity>> listFeeRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(feeRecordService.listFeeRecords(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/fee-records/{id}")
    public R<FeeRecordEntity> getFeeRecord(@PathVariable Long id) {
        return R.ok(feeRecordService.getFeeRecord(id));
    }

    @OperLog(module = "cdr", operation = "createFeeRecord")
    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/fee-records")
    public R<FeeRecordEntity> createFeeRecord(@RequestBody FeeRecordEntity entity) {
        return R.ok(feeRecordService.createFeeRecord(entity));
    }

    @OperLog(module = "cdr", operation = "deleteFeeRecord")
    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/fee-records/{id}")
    public R<Void> deleteFeeRecord(@PathVariable Long id) {
        feeRecordService.deleteFeeRecord(id);
        return R.ok();
    }
}
