# Plan 01: 后端实现

**Goal:** ClinicalSearchDomain 枚举 + DTO + Specification + Service + Controller

**依赖:** 无（复用现有 Repository）

---

## Task 1: DTO 类

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/ClinicalSearchDomain.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/ClinicalSearchRequest.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/ClinicalSearchResult.java`

- [ ] **Step 1: 创建 ClinicalSearchDomain 枚举**

```java
package com.maidc.data.dto;

public enum ClinicalSearchDomain {
    PATIENT, ENCOUNTER, DIAGNOSIS, LAB, MEDICATION,
    IMAGING, SURGERY, PATHOLOGY, VITAL, ALLERGY, NOTE
}
```

- [ ] **Step 2: 创建 ClinicalSearchRequest**

```java
package com.maidc.data.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class ClinicalSearchRequest {
    private String keyword;
    private ClinicalSearchDomain domain;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String department;
    private String diagnosis;
    private String status;
    private int page = 1;
    private int pageSize = 20;
}
```

- [ ] **Step 3: 创建 ClinicalSearchResult**

```java
package com.maidc.data.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ClinicalSearchResult {
    private String domain;
    private List<Map<String, Object>> items;
    private long total;
    private int page;
    private int pageSize;
}
```

- [ ] **Step 4: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/ClinicalSearchDomain.java \
  maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/ClinicalSearchRequest.java \
  maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/ClinicalSearchResult.java
git commit -m "feat(cdr-search): add clinical search DTOs and domain enum"
```

---

## Task 2: ClinicalSearchSpecs

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/spec/ClinicalSearchSpecs.java`

- [ ] **Step 1: 创建 Specification 构建器**

```java
package com.maidc.data.service.spec;

import com.maidc.data.dto.ClinicalSearchRequest;
import com.maidc.data.entity.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ClinicalSearchSpecs {

    public static Specification<PatientEntity> patientSpec(ClinicalSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String like = "%" + req.getKeyword() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("name"), like),
                    cb.like(root.get("phoneHash"), like)
                ));
            }
            applyDateRange(predicates, root, cb, req, "createdAt");
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<EncounterEntity> encounterSpec(ClinicalSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String like = "%" + req.getKeyword() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("attendingDoctor"), like),
                    cb.like(root.get("diagnosisSummary"), like)
                ));
            }
            if (req.getDepartment() != null && !req.getDepartment().isBlank()) {
                predicates.add(cb.equal(root.get("department"), req.getDepartment()));
            }
            if (req.getStatus() != null && !req.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("encounterType"), req.getStatus()));
            }
            applyDateRange(predicates, root, cb, req, "admissionTime");
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<DiagnosisEntity> diagnosisSpec(ClinicalSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String like = "%" + req.getKeyword() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("diagnosisName"), like),
                    cb.like(root.get("diagnosisCode"), like)
                ));
            }
            if (req.getDiagnosis() != null && !req.getDiagnosis().isBlank()) {
                predicates.add(cb.like(root.get("diagnosisName"), "%" + req.getDiagnosis() + "%"));
            }
            if (req.getStatus() != null && !req.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("diagnosisType"), req.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<LabTestEntity> labSpec(ClinicalSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String like = "%" + req.getKeyword() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("testName"), like),
                    cb.like(root.get("testCode"), like),
                    cb.like(root.get("orderingDoctor"), like)
                ));
            }
            applyDateRange(predicates, root, cb, req, "reportedAt");
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<MedicationEntity> medicationSpec(ClinicalSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String like = "%" + req.getKeyword() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("medName"), like),
                    cb.like(root.get("medCode"), like),
                    cb.like(root.get("prescriber"), like)
                ));
            }
            applyDateRange(predicates, root, cb, req, "startTime");
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<ImagingExamEntity> imagingSpec(ClinicalSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String like = "%" + req.getKeyword() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("examType"), like),
                    cb.like(root.get("bodyPart"), like),
                    cb.like(root.get("reportText"), like)
                ));
            }
            applyDateRange(predicates, root, cb, req, "studyDate");
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<OperationEntity> surgerySpec(ClinicalSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String like = "%" + req.getKeyword() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("operationName"), like),
                    cb.like(root.get("operationCode"), like),
                    cb.like(root.get("surgeon"), like)
                ));
            }
            applyDateRange(predicates, root, cb, req, "operatedAt");
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<PathologyEntity> pathologySpec(ClinicalSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String like = "%" + req.getKeyword() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("diagnosisDesc"), like),
                    cb.like(root.get("specimenType"), like)
                ));
            }
            applyDateRange(predicates, root, cb, req, "reportDate");
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<VitalSignEntity> vitalSpec(ClinicalSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                predicates.add(cb.like(root.get("signType"), "%" + req.getKeyword() + "%"));
            }
            applyDateRange(predicates, root, cb, req, "measuredAt");
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<AllergyEntity> allergySpec(ClinicalSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String like = "%" + req.getKeyword() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("allergen"), like),
                    cb.like(root.get("reaction"), like)
                ));
            }
            if (req.getStatus() != null && !req.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("severity"), req.getStatus()));
            }
            applyDateRange(predicates, root, cb, req, "confirmedAt");
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<ClinicalNoteEntity> noteSpec(ClinicalSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String like = "%" + req.getKeyword() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("title"), like),
                    cb.like(root.get("content"), like),
                    cb.like(root.get("author"), like)
                ));
            }
            if (req.getStatus() != null && !req.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("noteType"), req.getStatus()));
            }
            applyDateRange(predicates, root, cb, req, "noteDate");
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void applyDateRange(List<Predicate> predicates,
                                        jakarta.persistence.criteria.Path<?> root,
                                        jakarta.persistence.criteria.CriteriaBuilder cb,
                                        ClinicalSearchRequest req,
                                        String field) {
        if (req.getDateFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get(field), req.getDateFrom().atStartOfDay()));
        }
        if (req.getDateTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get(field), req.getDateTo().atTime(23, 59, 59)));
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/spec/ClinicalSearchSpecs.java
git commit -m "feat(cdr-search): add ClinicalSearchSpecs with 11 domain specifications"
```

---

## Task 3: ClinicalSearchService

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/ClinicalSearchService.java`

- [ ] **Step 1: 创建 ClinicalSearchService**

```java
package com.maidc.data.service;

import com.maidc.data.dto.ClinicalSearchDomain;
import com.maidc.data.dto.ClinicalSearchRequest;
import com.maidc.data.dto.ClinicalSearchResult;
import com.maidc.data.entity.PatientEntity;
import com.maidc.data.repository.*;
import com.maidc.data.service.spec.ClinicalSearchSpecs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
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

    // ---- PATIENT ----

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
        }).toList());
    }

    // ---- ENCOUNTER ----

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
        }).toList());
    }

    // ---- DIAGNOSIS ----

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
        }).toList());
    }

    // ---- LAB ----

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
        }).toList());
    }

    // ---- MEDICATION ----

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
        }).toList());
    }

    // ---- IMAGING ----

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
        }).toList());
    }

    // ---- SURGERY ----

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
        }).toList());
    }

    // ---- PATHOLOGY ----

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
        }).toList());
    }

    // ---- VITAL ----

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
        }).toList());
    }

    // ---- ALLERGY ----

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
        }).toList());
    }

    // ---- NOTE ----

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
        }).toList());
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/ClinicalSearchService.java
git commit -m "feat(cdr-search): add ClinicalSearchService with 11 domain search handlers"
```

---

## Task 4: ClinicalSearchController

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/ClinicalSearchController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.dto.ClinicalSearchRequest;
import com.maidc.data.dto.ClinicalSearchResult;
import com.maidc.data.service.ClinicalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cdr/search")
@RequiredArgsConstructor
public class ClinicalSearchController {

    private final ClinicalSearchService clinicalSearchService;

    @PreAuthorize("hasPermission('cdr:read')")
    @PostMapping
    public R<ClinicalSearchResult> search(@RequestBody ClinicalSearchRequest request) {
        if (request.getDomain() == null) {
            return R.fail(400, "搜索域(domain)不能为空");
        }
        return R.ok(clinicalSearchService.search(request));
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/ClinicalSearchController.java
git commit -m "feat(cdr-search): add ClinicalSearchController with POST /api/v1/cdr/search"
```

---

## Task 5: 构建验证

- [ ] **Step 1: 编译验证**

```bash
cd maidc-parent && mvn compile -pl maidc-data -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 如有编译错误，修复后重新提交**

常见问题：
- Repository 名称不匹配（确认所有 Repository 接口名）
- Entity import 路径错误
- JpaSpecificationExecutor 未被某些 Repository 继承（需添加 extends）
