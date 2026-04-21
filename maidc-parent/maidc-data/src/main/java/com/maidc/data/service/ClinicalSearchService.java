package com.maidc.data.service;

import com.maidc.data.dto.ClinicalSearchRequest;
import com.maidc.data.dto.ClinicalSearchResult;
import com.maidc.data.entity.*;
import com.maidc.data.repository.*;
import com.maidc.data.service.spec.ClinicalSearchSpecs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicalSearchService {

    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final LabTestRepository labTestRepository;
    private final MedicationRepository medicationRepository;
    private final ImagingExamRepository imagingExamRepository;
    private final OperationRepository operationRepository;
    private final PathologyRepository pathologyRepository;
    private final VitalSignRepository vitalSignRepository;
    private final AllergyRepository allergyRepository;
    private final ClinicalNoteRepository clinicalNoteRepository;

    public ClinicalSearchResult search(ClinicalSearchRequest req) {
        ClinicalSearchResult result = new ClinicalSearchResult();
        result.setDomain(req.getDomain().name());
        result.setPage(req.getPage());
        result.setPageSize(req.getPageSize());

        PageRequest pageable = PageRequest.of(req.getPage() - 1, req.getPageSize());

        switch (req.getDomain()) {
            case PATIENT -> searchPatients(req, pageable, result);
            case ENCOUNTER -> searchEncounters(req, pageable, result);
            case DIAGNOSIS -> searchDiagnosis(req, pageable, result);
            case LAB -> searchLabTests(req, pageable, result);
            case MEDICATION -> searchMedications(req, pageable, result);
            case IMAGING -> searchImaging(req, pageable, result);
            case SURGERY -> searchSurgery(req, pageable, result);
            case PATHOLOGY -> searchPathology(req, pageable, result);
            case VITAL -> searchVital(req, pageable, result);
            case ALLERGY -> searchAllergy(req, pageable, result);
            case NOTE -> searchNotes(req, pageable, result);
        }
        return result;
    }

    private Map<Long, String> resolvePatientNames(Set<Long> patientIds) {
        if (patientIds.isEmpty()) return Map.of();
        return patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(PatientEntity::getId, PatientEntity::getName));
    }

    private void searchPatients(ClinicalSearchRequest req, PageRequest pageable, ClinicalSearchResult result) {
        Page<PatientEntity> page = patientRepository.findAll(
                ClinicalSearchSpecs.patientSpec(req), pageable);
        result.setTotal(page.getTotalElements());
        result.setItems(page.getContent().stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("patientId", p.getId());
            map.put("patientName", p.getName());
            map.put("gender", p.getGender());
            map.put("birthDate", p.getBirthDate());
            map.put("createdAt", p.getCreatedAt());
            return map;
        }).collect(Collectors.toList()));
    }

    private void searchEncounters(ClinicalSearchRequest req, PageRequest pageable, ClinicalSearchResult result) {
        Page<EncounterEntity> page = encounterRepository.findAll(
                ClinicalSearchSpecs.encounterSpec(req), pageable);
        result.setTotal(page.getTotalElements());
        Map<Long, String> nameMap = resolvePatientNames(
                page.getContent().stream().map(EncounterEntity::getPatientId).collect(Collectors.toSet()));
        result.setItems(page.getContent().stream().map(e -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", e.getId());
            map.put("patientId", e.getPatientId());
            map.put("patientName", nameMap.getOrDefault(e.getPatientId(), "-"));
            map.put("encounterType", e.getEncounterType());
            map.put("department", e.getDepartment());
            map.put("attendingDoctor", e.getAttendingDoctor());
            map.put("diagnosisSummary", e.getDiagnosisSummary());
            map.put("admissionTime", e.getAdmissionTime());
            return map;
        }).collect(Collectors.toList()));
    }

    private void searchDiagnosis(ClinicalSearchRequest req, PageRequest pageable, ClinicalSearchResult result) {
        Page<DiagnosisEntity> page = diagnosisRepository.findAll(
                ClinicalSearchSpecs.diagnosisSpec(req), pageable);
        result.setTotal(page.getTotalElements());
        Map<Long, String> nameMap = resolvePatientNames(
                page.getContent().stream().map(DiagnosisEntity::getPatientId).collect(Collectors.toSet()));
        result.setItems(page.getContent().stream().map(d -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", d.getId());
            map.put("patientId", d.getPatientId());
            map.put("patientName", nameMap.getOrDefault(d.getPatientId(), "-"));
            map.put("diagnosisCode", d.getDiagnosisCode());
            map.put("diagnosisName", d.getDiagnosisName());
            map.put("diagnosisType", d.getDiagnosisType());
            map.put("encounterId", d.getEncounterId());
            return map;
        }).collect(Collectors.toList()));
    }

    private void searchLabTests(ClinicalSearchRequest req, PageRequest pageable, ClinicalSearchResult result) {
        Page<LabTestEntity> page = labTestRepository.findAll(
                ClinicalSearchSpecs.labSpec(req), pageable);
        result.setTotal(page.getTotalElements());
        Map<Long, String> nameMap = resolvePatientNames(
                page.getContent().stream().map(LabTestEntity::getPatientId).collect(Collectors.toSet()));
        result.setItems(page.getContent().stream().map(l -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", l.getId());
            map.put("patientId", l.getPatientId());
            map.put("patientName", nameMap.getOrDefault(l.getPatientId(), "-"));
            map.put("testName", l.getTestName());
            map.put("testCode", l.getTestCode());
            map.put("specimenType", l.getSpecimenType());
            map.put("status", l.getStatus());
            map.put("reportedAt", l.getReportedAt());
            return map;
        }).collect(Collectors.toList()));
    }

    private void searchMedications(ClinicalSearchRequest req, PageRequest pageable, ClinicalSearchResult result) {
        Page<MedicationEntity> page = medicationRepository.findAll(
                ClinicalSearchSpecs.medicationSpec(req), pageable);
        result.setTotal(page.getTotalElements());
        Map<Long, String> nameMap = resolvePatientNames(
                page.getContent().stream().map(MedicationEntity::getPatientId).collect(Collectors.toSet()));
        result.setItems(page.getContent().stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("patientId", m.getPatientId());
            map.put("patientName", nameMap.getOrDefault(m.getPatientId(), "-"));
            map.put("medName", m.getMedName());
            map.put("dosage", m.getDosage());
            map.put("route", m.getRoute());
            map.put("frequency", m.getFrequency());
            map.put("startTime", m.getStartTime());
            map.put("status", m.getStatus());
            return map;
        }).collect(Collectors.toList()));
    }

    private void searchImaging(ClinicalSearchRequest req, PageRequest pageable, ClinicalSearchResult result) {
        Page<ImagingExamEntity> page = imagingExamRepository.findAll(
                ClinicalSearchSpecs.imagingSpec(req), pageable);
        result.setTotal(page.getTotalElements());
        Map<Long, String> nameMap = resolvePatientNames(
                page.getContent().stream().map(ImagingExamEntity::getPatientId).collect(Collectors.toSet()));
        result.setItems(page.getContent().stream().map(i -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", i.getId());
            map.put("patientId", i.getPatientId());
            map.put("patientName", nameMap.getOrDefault(i.getPatientId(), "-"));
            map.put("examType", i.getExamType());
            map.put("bodyPart", i.getBodyPart());
            map.put("modality", i.getModality());
            map.put("status", i.getStatus());
            map.put("studyDate", i.getStudyDate());
            return map;
        }).collect(Collectors.toList()));
    }

    private void searchSurgery(ClinicalSearchRequest req, PageRequest pageable, ClinicalSearchResult result) {
        Page<OperationEntity> page = operationRepository.findAll(
                ClinicalSearchSpecs.surgerySpec(req), pageable);
        result.setTotal(page.getTotalElements());
        Map<Long, String> nameMap = resolvePatientNames(
                page.getContent().stream().map(OperationEntity::getPatientId).collect(Collectors.toSet()));
        result.setItems(page.getContent().stream().map(o -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", o.getId());
            map.put("patientId", o.getPatientId());
            map.put("patientName", nameMap.getOrDefault(o.getPatientId(), "-"));
            map.put("operationName", o.getOperationName());
            map.put("surgeon", o.getSurgeon());
            map.put("anesthesiaType", o.getAnesthesiaType());
            map.put("operatedAt", o.getOperatedAt());
            return map;
        }).collect(Collectors.toList()));
    }

    private void searchPathology(ClinicalSearchRequest req, PageRequest pageable, ClinicalSearchResult result) {
        Page<PathologyEntity> page = pathologyRepository.findAll(
                ClinicalSearchSpecs.pathologySpec(req), pageable);
        result.setTotal(page.getTotalElements());
        Map<Long, String> nameMap = resolvePatientNames(
                page.getContent().stream().map(PathologyEntity::getPatientId).collect(Collectors.toSet()));
        result.setItems(page.getContent().stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("patientId", p.getPatientId());
            map.put("patientName", nameMap.getOrDefault(p.getPatientId(), "-"));
            map.put("specimenType", p.getSpecimenType());
            map.put("diagnosisDesc", p.getDiagnosisDesc());
            map.put("grade", p.getGrade());
            map.put("stage", p.getStage());
            map.put("reportDate", p.getReportDate());
            return map;
        }).collect(Collectors.toList()));
    }

    private void searchVital(ClinicalSearchRequest req, PageRequest pageable, ClinicalSearchResult result) {
        Page<VitalSignEntity> page = vitalSignRepository.findAll(
                ClinicalSearchSpecs.vitalSpec(req), pageable);
        result.setTotal(page.getTotalElements());
        Map<Long, String> nameMap = resolvePatientNames(
                page.getContent().stream().map(VitalSignEntity::getPatientId).collect(Collectors.toSet()));
        result.setItems(page.getContent().stream().map(v -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", v.getId());
            map.put("patientId", v.getPatientId());
            map.put("patientName", nameMap.getOrDefault(v.getPatientId(), "-"));
            map.put("signType", v.getSignType());
            map.put("signValue", v.getSignValue());
            map.put("unit", v.getUnit());
            map.put("measuredAt", v.getMeasuredAt());
            return map;
        }).collect(Collectors.toList()));
    }

    private void searchAllergy(ClinicalSearchRequest req, PageRequest pageable, ClinicalSearchResult result) {
        Page<AllergyEntity> page = allergyRepository.findAll(
                ClinicalSearchSpecs.allergySpec(req), pageable);
        result.setTotal(page.getTotalElements());
        Map<Long, String> nameMap = resolvePatientNames(
                page.getContent().stream().map(AllergyEntity::getPatientId).collect(Collectors.toSet()));
        result.setItems(page.getContent().stream().map(a -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("patientId", a.getPatientId());
            map.put("patientName", nameMap.getOrDefault(a.getPatientId(), "-"));
            map.put("allergen", a.getAllergen());
            map.put("allergenType", a.getAllergenType());
            map.put("reaction", a.getReaction());
            map.put("severity", a.getSeverity());
            map.put("confirmedAt", a.getConfirmedAt());
            return map;
        }).collect(Collectors.toList()));
    }

    private void searchNotes(ClinicalSearchRequest req, PageRequest pageable, ClinicalSearchResult result) {
        Page<ClinicalNoteEntity> page = clinicalNoteRepository.findAll(
                ClinicalSearchSpecs.noteSpec(req), pageable);
        result.setTotal(page.getTotalElements());
        Map<Long, String> nameMap = resolvePatientNames(
                page.getContent().stream().map(ClinicalNoteEntity::getPatientId).collect(Collectors.toSet()));
        result.setItems(page.getContent().stream().map(n -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", n.getId());
            map.put("patientId", n.getPatientId());
            map.put("patientName", nameMap.getOrDefault(n.getPatientId(), "-"));
            map.put("noteType", n.getNoteType());
            map.put("title", n.getTitle());
            map.put("author", n.getAuthor());
            map.put("noteDate", n.getNoteDate());
            return map;
        }).collect(Collectors.toList()));
    }
}
