# Sub-project 3: Data Management CDR + RDR — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Complete CDR/RDR frontend alignment + create 35 new Entity/Repository/Service/Controller for all database tables (8 already existed).

**Status:** COMPLETED ✓ (2026-04-11)

**Architecture:** Frontend: minor fixes to 5 pages. Backend: Spring Boot JPA entities following existing patterns (BaseEntity with audit fields, soft delete, org_id multi-tenancy, NO foreign keys).

**Tech Stack:** Vue 3 + Ant Design Vue (frontend) | Spring Boot + Spring Data JPA + PostgreSQL (backend)

---

## Part A: Frontend Fixes (5 pages)

### Task 1: Patient 360 View redesign
- File: `maidc-portal/src/views/data-cdr/PatientDetail.vue`
- Design shows: Patient profile card + 4 metric stat cards + medical timeline + 5 tabs
- Current: Basic tab layout
- Action: Rewrite with profile header, metric cards, timeline component

### Task 2: Research Project List → Card grid
- File: `maidc-portal/src/views/data-rdr/ProjectList.vue`
- Design shows: Card grid (not table) with status badges, PI, timeline, team size
- Action: Rewrite with card grid layout

### Task 3: Minor fixes (3 pages)
- `DatasetList.vue` — add version count display
- `EtlTaskList.vue` — add cron expression display
- `FeatureDictionary.vue` — add colored category tags

### Task 4: Frontend build verification
- Run `npx vite build` from maidc-portal

---

## Part B: Backend Entities (41 new entities)

### Entity Pattern (follow existing codebase)
Every entity follows this pattern:
```java
@Entity
@Table(name = "table_name", schema = "schema_name")
@SQLDelete(sql = "UPDATE schema.table_name SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class XxxEntity extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // fields...
}
```

BaseEntity provides: createdBy, createdAt, updatedBy, updatedAt, isDeleted, orgId

No FK constraints — relationship columns are plain Long fields.

### Task 5: CDR Entities Batch 1 (Patient core — 6 entities)
Module: `maidc-data`

Create for each entity: `Entity.java` + `Repository.java` + `Service.java` + `Controller.java`

1. **EncounterEntity** — c_encounter (patient_id, encounter_no, encounter_type, dept_code, dept_name, doctor_code, doctor_name, admit_time, discharge_time, bed_no, ward_code, diagnosis_code, diagnosis_name, severity, status)
2. **DiagnosisEntity** — c_diagnosis (encounter_id, patient_id, diagnosis_type, icd_code, icd_name, diagnosis_time, doctor_code, sort_order)
3. **LabTestEntity** — c_lab_test (encounter_id, patient_id, test_code, test_name, specimen_type, ordered_at, collected_at, reported_at, status, ordering_doctor)
4. **LabPanelEntity** — c_lab_panel (test_id, item_code, item_name, result_value, result_unit, reference_range, abnormal_flag)
5. **MedicationEntity** — c_medication (encounter_id, patient_id, med_code, med_name, dosage, route, frequency, start_time, end_time, prescriber, status)
6. **VitalSignEntity** — c_vital_sign (encounter_id, patient_id, sign_type, sign_value, unit, measured_at, measured_by)

### Task 6: CDR Entities Batch 2 (Imaging + Pathology — 5 entities)
7. **ImagingExamEntity** — c_imaging_exam (encounter_id, patient_id, accession_no, exam_type, body_part, study_date, modality, status, report_text, dicom_bucket_path)
8. **ImagingFindingEntity** — c_imaging_finding (exam_id, finding_type, finding_desc, laterality, severity, region, annotation_data JSONB)
9. **PathologyEntity** — c_pathology (encounter_id, patient_id, specimen_no, specimen_type, diagnosis_desc, grade, stage, report_date)
10. **OperationEntity** — c_operation (encounter_id, patient_id, operation_code, operation_name, operated_at, duration_min, surgeon, assistant, anesthesia_type)
11. **AllergyEntity** — c_allergy (patient_id, allergen, allergen_type, reaction, severity, confirmed_at)

### Task 7: CDR Entities Batch 3 (Other clinical — 10 entities)
12. **FamilyHistoryEntity** — c_family_history (patient_id, relationship, disease_name, icd_code, onset_age)
13. **ClinicalNoteEntity** — c_clinical_note (encounter_id, patient_id, note_type, title, content, author, note_date)
14. **DischargeSummaryEntity** — c_discharge_summary (encounter_id, patient_id, admission_diagnosis, discharge_diagnosis, treatment_summary, discharge_instruction, follow_up_plan, discharged_at, discharge_doctor)
15. **PatientContactEntity** — c_patient_contact (patient_id, contact_name, relationship, phone, address, is_emergency)
16. **PatientInsuranceEntity** — c_patient_insurance (patient_id, insurance_type, insurance_no, holder_name, valid_from, valid_to)
17. **PatientBedEntity** — c_patient_bed (patient_id, encounter_id, bed_no, ward_code, building, floor, assigned_at, vacated_at)
18. **NursingRecordEntity** — c_nursing_record (encounter_id, patient_id, record_type, content, nurse_code, record_time)
19. **BloodTransfusionEntity** — c_blood_transfusion (encounter_id, patient_id, blood_type, volume_ml, transfused_at, operator, reaction_desc)
20. **TransferEntity** — c_transfer (encounter_id, patient_id, from_dept, to_dept, transfer_type, transfer_time, reason)
21. **FeeRecordEntity** — c_fee_record (encounter_id, patient_id, fee_type, fee_name, amount, quantity, billing_time)

### Task 8: CDR Entities Batch 4 (Health checkup + Org — 7 entities)
22. **HealthCheckupEntity** — c_health_checkup (patient_id, checkup_no, checkup_date, org_name, conclusion, suggestion)
23. **CheckupPackageEntity** — c_checkup_package (checkup_id, package_name, category)
24. **CheckupItemResultEntity** — c_checkup_item_result (package_id, item_code, item_name, result_value, unit, reference_range, abnormal_flag)
25. **CheckupSummaryEntity** — c_checkup_summary (checkup_id, summary_type, summary_content)
26. **CheckupComparisonEntity** — c_checkup_comparison (patient_id, current_checkup_id, previous_checkup_id, comparison_result JSONB)
27. **OrgEntity** — c_org (org_code, org_name, org_type, parent_id, address, contact_phone, status)
28. **ClinicalNoteEntity** already covered — skip if duplicate

### Task 9: RDR Entities Batch 1 (Project + Cohort — 4 entities)
Module: `maidc-data`

29. **StudyMemberEntity** — r_study_member (project_id, user_id, role, joined_at, status)
30. **ResearchCohortEntity** — r_research_cohort (project_id, cohort_name, description, inclusion_criteria JSONB, exclusion_criteria JSONB, target_size, current_size, status)
31. **StudySubjectEntity** — r_study_subject (project_id, cohort_id, patient_id, subject_no, enrollment_date, withdrawal_date, status)
32. **DatasetVersionEntity** — r_dataset_version (dataset_id, version_no, changelog, file_path, file_size, checksum, record_count, status)

### Task 10: RDR Entities Batch 2 (Specialized datasets — 6 entities)
33. **DatasetAccessLogEntity** — r_dataset_access_log (dataset_id, user_id, access_type, purpose, record_count, accessed_at)
34. **ClinicalFeatureEntity** — r_clinical_feature (project_id, feature_code, feature_name, data_type, source_table, source_column, unit, value_range JSONB, description)
35. **FeatureDictionaryEntity** — r_feature_dictionary (feature_name, feature_category, data_type, standard_code, standard_system, description, is_enabled)
36. **ImagingDatasetEntity** — r_imaging_dataset (dataset_id, exam_id, image_format, resolution JSONB, body_part, annotation_status)
37. **ImagingAnnotationEntity** — r_imaging_annotation (imaging_dataset_id, annotator_id, annotation_type, annotation_data JSONB, status)
38. **GenomicDatasetEntity** — r_genomic_dataset (dataset_id, sample_id, genome_build, file_format, file_path)

### Task 11: RDR Entities Batch 3 (Genomic + Text + ETL + Quality — 8 entities)
39. **GenomicVariantEntity** — r_genomic_variant (genomic_dataset_id, chromosome, position, ref_allele, alt_allele, variant_type, gene_symbol, significance)
40. **TextDatasetEntity** — r_text_dataset (dataset_id, source_type, text_content, language)
41. **TextAnnotationEntity** — r_text_annotation (text_dataset_id, annotator_id, annotation_type, entities JSONB, status)
42. **EtlTaskEntity** — r_etl_task (project_id, task_name, source_config JSONB, target_config JSONB, transform_config JSONB, schedule_cron, status, last_run_at, next_run_at)
43. **EtlTaskLogEntity** — r_etl_task_log (task_id, run_no, start_time, end_time, status, records_read, records_written, error_count, error_message, log_file_path)
44. **DataQualityRuleEntity** — r_data_quality_rule (project_id, rule_name, rule_type, target_table, target_column, rule_expr JSONB, threshold, severity, enabled)
45. **DataQualityResultEntity** — r_data_quality_result (rule_id, dataset_id, total_records, passed_records, failed_records, pass_rate, detail JSONB, checked_at)

### Task 12: Backend build verification
- Run `mvn clean compile` from maidc-parent
- Fix any compilation errors

---

## File Organization

All new entities go into the `maidc-data` module following existing package structure:
```
maidc-data/src/main/java/com/maidc/data/
├── entity/
│   ├── cdr/          (28 CDR entities)
│   └── rdr/          (16 RDR entities)
├── repository/
│   ├── cdr/
│   └── rdr/
├── service/
│   ├── cdr/
│   └── rdr/
└── controller/
    ├── cdr/
    └── rdr/
```

**Note:** Check existing entity package structure before creating — some entities may already exist in a flat entity/ package.
