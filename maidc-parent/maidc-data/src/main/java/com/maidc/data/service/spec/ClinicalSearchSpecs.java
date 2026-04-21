package com.maidc.data.service.spec;

import com.maidc.data.dto.ClinicalSearchRequest;
import com.maidc.data.entity.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

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
                predicates.add(cb.like(root.get("department"), "%" + req.getDepartment() + "%"));
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
