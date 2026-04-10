package com.maidc.data.controller;

import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.data.dto.PatientCreateDTO;
import com.maidc.data.dto.PatientQueryDTO;
import com.maidc.data.service.EncounterService;
import com.maidc.data.service.Patient360Service;
import com.maidc.data.service.PatientService;
import com.maidc.data.vo.EncounterVO;
import com.maidc.data.vo.PatientDetailVO;
import com.maidc.data.vo.PatientVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
