-- ============================================================
-- CDR (Clinical Data Repository) Schema DDL
-- 28 tables for patient clinical data tracking
-- ============================================================

-- 1. Patient Demographics
CREATE TABLE IF NOT EXISTS cdr.c_patient (
    id                  BIGSERIAL       PRIMARY KEY,
    patient_no          VARCHAR(32)     NOT NULL,
    id_card_no          VARCHAR(32)     NOT NULL,   -- encrypted
    name                VARCHAR(64)     NOT NULL,   -- encrypted
    gender              CHAR(1)         NOT NULL CHECK (gender IN ('M', 'F', 'O')),
    birth_date          DATE,
    ethnicity           VARCHAR(16),
    marital_status      VARCHAR(16),
    occupation          VARCHAR(32),
    blood_type          VARCHAR(8),
    address             TEXT,                       -- encrypted
    phone               VARCHAR(32),                -- encrypted
    source_system       VARCHAR(32),
    source_id           VARCHAR(64),
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL,
    UNIQUE (patient_no, org_id)
);

CREATE INDEX idx_c_patient_source ON cdr.c_patient (source_system, source_id);

COMMENT ON TABLE cdr.c_patient IS 'Patient demographics and identity information';

-- 2. Clinical Encounters
CREATE TABLE IF NOT EXISTS cdr.c_encounter (
    id                  BIGSERIAL       PRIMARY KEY,
    patient_id          BIGINT          NOT NULL,
    encounter_no        VARCHAR(32)     NOT NULL,
    encounter_type      VARCHAR(16)     NOT NULL CHECK (encounter_type IN ('OUTPATIENT', 'INPATIENT', 'EMERGENCY')),
    dept_code           VARCHAR(32),
    dept_name           VARCHAR(64),
    doctor_code         VARCHAR(32),
    doctor_name         VARCHAR(64),
    admit_time          TIMESTAMP,
    discharge_time      TIMESTAMP,
    bed_no              VARCHAR(16),
    ward_code           VARCHAR(32),
    diagnosis_code      VARCHAR(32),
    diagnosis_name      VARCHAR(128),
    severity            VARCHAR(16),
    status              VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL,
    UNIQUE (encounter_no, org_id)
);

CREATE INDEX idx_c_encounter_patient ON cdr.c_encounter (patient_id);
CREATE INDEX idx_c_encounter_admit ON cdr.c_encounter (admit_time);

COMMENT ON TABLE cdr.c_encounter IS 'Clinical encounters including outpatient, inpatient and emergency visits';

-- 3. Diagnoses
CREATE TABLE IF NOT EXISTS cdr.c_diagnosis (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    diagnosis_type      VARCHAR(16)     NOT NULL CHECK (diagnosis_type IN ('MAIN', 'SECONDARY', 'ADMISSION', 'DISCHARGE')),
    icd_code            VARCHAR(32),
    icd_name            VARCHAR(128),
    diagnosis_time      TIMESTAMP,
    doctor_code         VARCHAR(32),
    sort_order          INT,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_c_diagnosis_encounter ON cdr.c_diagnosis (encounter_id);
CREATE INDEX idx_c_diagnosis_icd ON cdr.c_diagnosis (icd_code);

COMMENT ON TABLE cdr.c_diagnosis IS 'Patient diagnoses linked to encounters with ICD coding';

-- 4. Lab Test Orders
CREATE TABLE IF NOT EXISTS cdr.c_lab_test (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    test_code           VARCHAR(32),
    test_name           VARCHAR(128),
    specimen_type       VARCHAR(32),
    ordered_at          TIMESTAMP,
    collected_at        TIMESTAMP,
    reported_at         TIMESTAMP,
    status              VARCHAR(16)     NOT NULL DEFAULT 'ORDERED',
    ordering_doctor     VARCHAR(32),
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_lab_test IS 'Laboratory test orders linked to clinical encounters';

-- 5. Lab Results
CREATE TABLE IF NOT EXISTS cdr.c_lab_panel (
    id                  BIGSERIAL       PRIMARY KEY,
    test_id             BIGINT          NOT NULL,
    item_code           VARCHAR(32),
    item_name           VARCHAR(64),
    result_value        TEXT,
    result_unit         VARCHAR(32),
    reference_range     VARCHAR(64),
    abnormal_flag       BOOLEAN         NOT NULL DEFAULT FALSE,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_lab_panel IS 'Individual lab result items within a test order';

-- 6. Medication Orders
CREATE TABLE IF NOT EXISTS cdr.c_medication (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    med_code            VARCHAR(32),
    med_name            VARCHAR(128),
    dosage              VARCHAR(64),
    route               VARCHAR(32),
    frequency           VARCHAR(32),
    start_time          TIMESTAMP,
    end_time            TIMESTAMP,
    prescriber          VARCHAR(32),
    status              VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_medication IS 'Medication orders and prescriptions for patients';

-- 7. Vital Signs
CREATE TABLE IF NOT EXISTS cdr.c_vital_sign (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    sign_type           VARCHAR(32)     NOT NULL CHECK (sign_type IN ('TEMPERATURE', 'PULSE', 'BP_SYSTOLIC', 'BP_DIASTOLIC', 'RESPIRATION', 'SPO2', 'HEIGHT', 'WEIGHT')),
    sign_value          DECIMAL(8,2),
    unit                VARCHAR(16),
    measured_at         TIMESTAMP,
    measured_by         VARCHAR(32),
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_vital_sign IS 'Patient vital sign measurements';

-- 8. Imaging Exams
CREATE TABLE IF NOT EXISTS cdr.c_imaging_exam (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    accession_no        VARCHAR(32),
    exam_type           VARCHAR(32)     NOT NULL CHECK (exam_type IN ('CT', 'MRI', 'XRAY', 'ULTRASOUND', 'PET')),
    body_part           VARCHAR(64),
    study_date          TIMESTAMP,
    modality            VARCHAR(16),
    status              VARCHAR(16)     NOT NULL DEFAULT 'ORDERED',
    report_text         TEXT,
    dicom_bucket_path   VARCHAR(256),
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_imaging_exam IS 'Imaging examination orders and reports';

-- 9. Imaging Findings
CREATE TABLE IF NOT EXISTS cdr.c_imaging_finding (
    id                  BIGSERIAL       PRIMARY KEY,
    exam_id             BIGINT          NOT NULL,
    finding_type        VARCHAR(32),
    finding_desc        TEXT,
    laterality          VARCHAR(16),
    severity            VARCHAR(16),
    region              VARCHAR(64),
    annotation_data     JSONB,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_imaging_finding IS 'Detailed findings from imaging examinations';

-- 10. Pathology
CREATE TABLE IF NOT EXISTS cdr.c_pathology (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    specimen_no         VARCHAR(32),
    specimen_type       VARCHAR(32),
    diagnosis_desc      TEXT,
    grade               VARCHAR(16),
    stage               VARCHAR(16),
    report_date         TIMESTAMP,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_pathology IS 'Pathology specimen reports and diagnoses';

-- 11. Operations
CREATE TABLE IF NOT EXISTS cdr.c_operation (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    operation_code      VARCHAR(32),
    operation_name      VARCHAR(128),
    operated_at         TIMESTAMP,
    duration_min        INT,
    surgeon             VARCHAR(32),
    assistant           VARCHAR(64),
    anesthesia_type     VARCHAR(32),
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_operation IS 'Surgical operation records';

-- 12. Allergies
CREATE TABLE IF NOT EXISTS cdr.c_allergy (
    id                  BIGSERIAL       PRIMARY KEY,
    patient_id          BIGINT          NOT NULL,
    allergen            VARCHAR(128),
    allergen_type       VARCHAR(32)     CHECK (allergen_type IN ('DRUG', 'FOOD', 'ENVIRONMENT')),
    reaction            VARCHAR(128),
    severity            VARCHAR(16)     CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE')),
    confirmed_at        TIMESTAMP,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_allergy IS 'Patient allergy records with allergen type and severity';

-- 13. Family History
CREATE TABLE IF NOT EXISTS cdr.c_family_history (
    id                  BIGSERIAL       PRIMARY KEY,
    patient_id          BIGINT          NOT NULL,
    relationship        VARCHAR(32),
    disease_name        VARCHAR(128),
    icd_code            VARCHAR(32),
    onset_age           INT,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_family_history IS 'Patient family disease history';

-- 14. Clinical Notes
CREATE TABLE IF NOT EXISTS cdr.c_clinical_note (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    note_type           VARCHAR(32)     NOT NULL CHECK (note_type IN ('ADMISSION', 'PROGRESS', 'DISCHARGE', 'CONSULTATION')),
    title               VARCHAR(128),
    content             TEXT,
    author              VARCHAR(64),
    note_date           TIMESTAMP,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_clinical_note IS 'Clinical notes including admission, progress, discharge and consultation notes';

-- 15. Organizations
CREATE TABLE IF NOT EXISTS cdr.c_org (
    id                  BIGSERIAL       PRIMARY KEY,
    org_code            VARCHAR(32)     NOT NULL,
    org_name            VARCHAR(128),
    org_type            VARCHAR(16)     CHECK (org_type IN ('HOSPITAL', 'CLINIC', 'RESEARCH')),
    parent_id           BIGINT,
    address             TEXT,
    contact_phone       VARCHAR(32),
    status              VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL,
    UNIQUE (org_code, org_id)
);

COMMENT ON TABLE cdr.c_org IS 'Healthcare organizations with hierarchical structure';

-- 16. Health Checkups
CREATE TABLE IF NOT EXISTS cdr.c_health_checkup (
    id                  BIGSERIAL       PRIMARY KEY,
    patient_id          BIGINT          NOT NULL,
    checkup_no          VARCHAR(32),
    checkup_date        DATE,
    org_name            VARCHAR(128),
    conclusion          TEXT,
    suggestion          TEXT,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_health_checkup IS 'Health checkup examination records';

-- 17. Checkup Packages
CREATE TABLE IF NOT EXISTS cdr.c_checkup_package (
    id                  BIGSERIAL       PRIMARY KEY,
    checkup_id          BIGINT          NOT NULL,
    package_name        VARCHAR(128),
    category            VARCHAR(32),
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_checkup_package IS 'Checkup examination packages within a health checkup';

-- 18. Checkup Item Results
CREATE TABLE IF NOT EXISTS cdr.c_checkup_item_result (
    id                  BIGSERIAL       PRIMARY KEY,
    package_id          BIGINT          NOT NULL,
    item_code           VARCHAR(32),
    item_name           VARCHAR(64),
    result_value        TEXT,
    unit                VARCHAR(32),
    reference_range     VARCHAR(64),
    abnormal_flag       BOOLEAN         NOT NULL DEFAULT FALSE,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_checkup_item_result IS 'Individual item results within a checkup package';

-- 19. Checkup Summaries
CREATE TABLE IF NOT EXISTS cdr.c_checkup_summary (
    id                  BIGSERIAL       PRIMARY KEY,
    checkup_id          BIGINT          NOT NULL,
    summary_type        VARCHAR(32),
    summary_content     TEXT,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_checkup_summary IS 'Summary conclusions for health checkup examinations';

-- 20. Checkup Comparisons
CREATE TABLE IF NOT EXISTS cdr.c_checkup_comparison (
    id                  BIGSERIAL       PRIMARY KEY,
    patient_id          BIGINT          NOT NULL,
    current_checkup_id  BIGINT          NOT NULL,
    previous_checkup_id BIGINT          NOT NULL,
    comparison_result   JSONB,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_checkup_comparison IS 'Comparison results between health checkups';

-- 21. Nursing Records
CREATE TABLE IF NOT EXISTS cdr.c_nursing_record (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    record_type         VARCHAR(32),
    content             TEXT,
    nurse_code          VARCHAR(32),
    record_time         TIMESTAMP,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_nursing_record IS 'Nursing care records linked to encounters';

-- 22. Blood Transfusions
CREATE TABLE IF NOT EXISTS cdr.c_blood_transfusion (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    blood_type          VARCHAR(16),
    volume_ml           INT,
    transfused_at       TIMESTAMP,
    operator            VARCHAR(32),
    reaction_desc       TEXT,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_blood_transfusion IS 'Blood transfusion records with reaction tracking';

-- 23. Department Transfers
CREATE TABLE IF NOT EXISTS cdr.c_transfer (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    from_dept           VARCHAR(64),
    to_dept             VARCHAR(64),
    transfer_type       VARCHAR(32)     CHECK (transfer_type IN ('DEPT', 'WARD', 'ICU')),
    transfer_time       TIMESTAMP,
    reason              TEXT,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_transfer IS 'Patient department/ward transfer records';

-- 24. Fee Records
CREATE TABLE IF NOT EXISTS cdr.c_fee_record (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    fee_type            VARCHAR(32),
    fee_name            VARCHAR(128),
    amount              DECIMAL(12,2),
    quantity            DECIMAL(8,2),
    billing_time        TIMESTAMP,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_fee_record IS 'Patient fee and billing records';

-- 25. Discharge Summaries
CREATE TABLE IF NOT EXISTS cdr.c_discharge_summary (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    admission_diagnosis TEXT,
    discharge_diagnosis TEXT,
    treatment_summary   TEXT,
    discharge_instruction TEXT,
    follow_up_plan      TEXT,
    discharged_at       TIMESTAMP,
    discharge_doctor    VARCHAR(32),
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_discharge_summary IS 'Discharge summaries with diagnosis, treatment and follow-up plans';

-- 26. Patient Contacts
CREATE TABLE IF NOT EXISTS cdr.c_patient_contact (
    id                  BIGSERIAL       PRIMARY KEY,
    patient_id          BIGINT          NOT NULL,
    contact_name        VARCHAR(64),
    relationship        VARCHAR(32),
    phone               VARCHAR(32),
    address             TEXT,
    is_emergency        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_patient_contact IS 'Patient emergency and general contact persons';

-- 27. Patient Insurance
CREATE TABLE IF NOT EXISTS cdr.c_patient_insurance (
    id                  BIGSERIAL       PRIMARY KEY,
    patient_id          BIGINT          NOT NULL,
    insurance_type      VARCHAR(32),
    insurance_no        VARCHAR(64),
    holder_name         VARCHAR(64),
    valid_from          DATE,
    valid_to            DATE,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_patient_insurance IS 'Patient medical insurance coverage records';

-- 28. Patient Bed Assignments
CREATE TABLE IF NOT EXISTS cdr.c_patient_bed (
    id                  BIGSERIAL       PRIMARY KEY,
    patient_id          BIGINT          NOT NULL,
    encounter_id        BIGINT          NOT NULL,
    bed_no              VARCHAR(16),
    ward_code           VARCHAR(32),
    building            VARCHAR(32),
    floor               VARCHAR(16),
    assigned_at         TIMESTAMP,
    vacated_at          TIMESTAMP,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_patient_bed IS 'Patient bed assignment and tracking records';
