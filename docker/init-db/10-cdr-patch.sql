-- ============================================================
-- CDR Patch - Extend existing tables + add new tables
-- Supports MIMIC-III and MIMIC-IV data model mapping
-- ============================================================

-- ===========================================================
-- Part A: ALTER existing CDR tables
-- ===========================================================

-- 1. c_patient: add MIMIC-specific demographic fields
ALTER TABLE cdr.c_patient ADD COLUMN IF NOT EXISTS dod DATE;
ALTER TABLE cdr.c_patient ADD COLUMN IF NOT EXISTS expire_flag BOOLEAN DEFAULT FALSE;
ALTER TABLE cdr.c_patient ADD COLUMN IF NOT EXISTS anchor_age INT;
ALTER TABLE cdr.c_patient ADD COLUMN IF NOT EXISTS anchor_year INT;
ALTER TABLE cdr.c_patient ADD COLUMN IF NOT EXISTS anchor_year_group VARCHAR(32);

COMMENT ON COLUMN cdr.c_patient.dod IS 'Date of death (from MIMIC DOD fields)';
COMMENT ON COLUMN cdr.c_patient.expire_flag IS 'Whether patient expired (hospital death)';
COMMENT ON COLUMN cdr.c_patient.anchor_age IS 'MIMIC-IV: age at anchor_year';
COMMENT ON COLUMN cdr.c_patient.anchor_year IS 'MIMIC-IV: reference year for date shifting';
COMMENT ON COLUMN cdr.c_patient.anchor_year_group IS 'MIMIC-IV: year group for the patient';

-- 2. c_encounter: add admission detail fields
ALTER TABLE cdr.c_encounter ADD COLUMN IF NOT EXISTS admission_type VARCHAR(32);
ALTER TABLE cdr.c_encounter ADD COLUMN IF NOT EXISTS admission_location VARCHAR(64);
ALTER TABLE cdr.c_encounter ADD COLUMN IF NOT EXISTS discharge_location VARCHAR(64);
ALTER TABLE cdr.c_encounter ADD COLUMN IF NOT EXISTS death_time TIMESTAMP;
ALTER TABLE cdr.c_encounter ADD COLUMN IF NOT EXISTS hospital_expire_flag BOOLEAN DEFAULT FALSE;
ALTER TABLE cdr.c_encounter ADD COLUMN IF NOT EXISTS insurance_type VARCHAR(32);
ALTER TABLE cdr.c_encounter ADD COLUMN IF NOT EXISTS language VARCHAR(32);
ALTER TABLE cdr.c_encounter ADD COLUMN IF NOT EXISTS ethnicity VARCHAR(64);
ALTER TABLE cdr.c_encounter ADD COLUMN IF NOT EXISTS ed_reg_time TIMESTAMP;
ALTER TABLE cdr.c_encounter ADD COLUMN IF NOT EXISTS ed_out_time TIMESTAMP;

COMMENT ON COLUMN cdr.c_encounter.admission_type IS 'Admission type: EMERGENCY, URGENT, ELECTIVE, NEWBORN, EU OBSERVATION etc.';
COMMENT ON COLUMN cdr.c_encounter.insurance_type IS 'Insurance type: Medicare, Medicaid, Private, Self Pay etc.';

-- 3. c_diagnosis: add ICD version support
ALTER TABLE cdr.c_diagnosis ADD COLUMN IF NOT EXISTS icd_version INT;

COMMENT ON COLUMN cdr.c_diagnosis.icd_version IS 'ICD code version: 9 or 10';

-- 4. c_vital_sign: expand sign_type from CHECK enum to free-form
-- Remove the restrictive CHECK constraint and expand to VARCHAR(64)
ALTER TABLE cdr.c_vital_sign DROP CONSTRAINT IF EXISTS c_vital_sign_sign_type_check;
ALTER TABLE cdr.c_vital_sign ALTER COLUMN sign_type TYPE VARCHAR(64);
ALTER TABLE cdr.c_vital_sign ADD COLUMN IF NOT EXISTS itemid INT;

COMMENT ON COLUMN cdr.c_vital_sign.itemid IS 'MIMIC item ID for cross-reference with D_ITEMS dictionary';

-- 5. c_lab_panel: add reference range and specimen detail
ALTER TABLE cdr.c_lab_panel ADD COLUMN IF NOT EXISTS ref_range_lower DECIMAL(16,6);
ALTER TABLE cdr.c_lab_panel ADD COLUMN IF NOT EXISTS ref_range_upper DECIMAL(16,6);
ALTER TABLE cdr.c_lab_panel ADD COLUMN IF NOT EXISTS priority VARCHAR(16);
ALTER TABLE cdr.c_lab_panel ADD COLUMN IF NOT EXISTS comments TEXT;
ALTER TABLE cdr.c_lab_panel ADD COLUMN IF NOT EXISTS specimen_id BIGINT;

COMMENT ON COLUMN cdr.c_lab_panel.ref_range_lower IS 'Reference range lower bound';
COMMENT ON COLUMN cdr.c_lab_panel.ref_range_upper IS 'Reference range upper bound';

-- 6. c_clinical_note: expand note_type
ALTER TABLE cdr.c_clinical_note DROP CONSTRAINT IF EXISTS c_clinical_note_note_type_check;
ALTER TABLE cdr.c_clinical_note ALTER COLUMN note_type TYPE VARCHAR(64);
ALTER TABLE cdr.c_clinical_note ADD COLUMN IF NOT EXISTS is_error BOOLEAN DEFAULT FALSE;
ALTER TABLE cdr.c_clinical_note ADD COLUMN IF NOT EXISTS note_category VARCHAR(64);

COMMENT ON COLUMN cdr.c_clinical_note.note_category IS 'MIMIC note category: Nursing, Radiology, General, ECG, Echo, Respiratory etc.';

-- ===========================================================
-- Part B: New CDR clinical tables (12 tables)
-- ===========================================================

-- B1. ICU Stay
CREATE TABLE IF NOT EXISTS cdr.c_icu_stay (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    stay_no             VARCHAR(32),            -- icustay_id (III) or stay_id (IV)
    first_careunit      VARCHAR(32),
    last_careunit       VARCHAR(32),
    first_ward_id       INT,
    last_ward_id        INT,
    in_time             TIMESTAMP,
    out_time            TIMESTAMP,
    los_days            DECIMAL(10,4),
    dbsource            VARCHAR(16),            -- MIMIC-III: carevue/metavision
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    source_id           VARCHAR(64),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_c_icu_stay_encounter ON cdr.c_icu_stay (encounter_id);
CREATE INDEX idx_c_icu_stay_patient ON cdr.c_icu_stay (patient_id);

COMMENT ON TABLE cdr.c_icu_stay IS 'ICU stay records mapped from MIMIC ICUSTAYS/icustays';

-- B2. Input Event (infusions, fluids, medications administered)
CREATE TABLE IF NOT EXISTS cdr.c_input_event (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    icu_stay_id         BIGINT,
    itemid              INT,
    start_time          TIMESTAMP,
    end_time            TIMESTAMP,
    amount              DECIMAL(16,6),
    amount_uom          VARCHAR(32),
    rate                DECIMAL(16,6),
    rate_uom            VARCHAR(32),
    patient_weight      DECIMAL(8,2),
    total_amount        DECIMAL(16,6),
    total_amount_uom    VARCHAR(32),
    order_id            BIGINT,
    link_order_id       BIGINT,
    order_category      VARCHAR(64),
    order_component_type VARCHAR(128),
    status              VARCHAR(32),
    caregiver_id        BIGINT,
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    source_id           VARCHAR(64),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_c_input_encounter ON cdr.c_input_event (encounter_id);
CREATE INDEX idx_c_input_patient ON cdr.c_input_event (patient_id, start_time);
CREATE INDEX idx_c_input_stay ON cdr.c_input_event (icu_stay_id);

COMMENT ON TABLE cdr.c_input_event IS 'Input/infusion events from MIMIC INPUTEVENTS_CV, INPUTEVENTS_MV, inputevents';

-- B3. Output Event (urine output, drainage)
CREATE TABLE IF NOT EXISTS cdr.c_output_event (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    icu_stay_id         BIGINT,
    itemid              INT,
    chart_time          TIMESTAMP,
    value_num           DECIMAL(16,6),
    value_uom           VARCHAR(32),
    caregiver_id        BIGINT,
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    source_id           VARCHAR(64),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_c_output_encounter ON cdr.c_output_event (encounter_id);
CREATE INDEX idx_c_output_patient ON cdr.c_output_event (patient_id);

COMMENT ON TABLE cdr.c_output_event IS 'Output events (urine, drains) from MIMIC OUTPUTEVENTS, outputevents';

-- B4. Microbiology (culture and sensitivity results)
CREATE TABLE IF NOT EXISTS cdr.c_microbiology (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    chart_date          TIMESTAMP,
    chart_time          TIMESTAMP,
    spec_itemid         INT,
    spec_type_desc      VARCHAR(128),
    test_itemid         INT,
    test_name           VARCHAR(128),
    org_itemid          INT,
    org_name            VARCHAR(256),
    isolate_num         INT,
    ab_itemid           INT,
    ab_name             VARCHAR(64),
    dilution_text       VARCHAR(32),
    dilution_comparison VARCHAR(16),
    dilution_value      DECIMAL(16,6),
    interpretation      VARCHAR(16),
    quantity            VARCHAR(32),
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    source_id           VARCHAR(64),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_c_micro_encounter ON cdr.c_microbiology (encounter_id);
CREATE INDEX idx_c_micro_patient ON cdr.c_microbiology (patient_id);
CREATE INDEX idx_c_micro_org ON cdr.c_microbiology (org_name);

COMMENT ON TABLE cdr.c_microbiology IS 'Microbiology culture and antibiotic sensitivity results';

-- B5. Procedure Event (bedside procedures)
CREATE TABLE IF NOT EXISTS cdr.c_procedure_event (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    icu_stay_id         BIGINT,
    itemid              INT,
    start_time          TIMESTAMP,
    end_time            TIMESTAMP,
    value_text          TEXT,
    value_uom           VARCHAR(32),
    location            VARCHAR(64),
    location_category   VARCHAR(64),
    order_id            BIGINT,
    link_order_id       BIGINT,
    order_category      VARCHAR(64),
    patient_weight      DECIMAL(8,2),
    status              VARCHAR(32),
    caregiver_id        BIGINT,
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    source_id           VARCHAR(64),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_c_proc_event_encounter ON cdr.c_procedure_event (encounter_id);
CREATE INDEX idx_c_proc_event_stay ON cdr.c_procedure_event (icu_stay_id);

COMMENT ON TABLE cdr.c_procedure_event IS 'Bedside procedure events from MIMIC PROCEDUREEVENTS_MV, procedureevents';

-- B6. Datetime Event (datetime-typed chart events)
CREATE TABLE IF NOT EXISTS cdr.c_datetime_event (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    icu_stay_id         BIGINT,
    itemid              INT,
    chart_time          TIMESTAMP,
    value_text          TEXT,
    value_uom           VARCHAR(32),
    warning             BOOLEAN,
    caregiver_id        BIGINT,
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    source_id           VARCHAR(64),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_c_dte_encounter ON cdr.c_datetime_event (encounter_id);
CREATE INDEX idx_c_dte_patient ON cdr.c_datetime_event (patient_id);

COMMENT ON TABLE cdr.c_datetime_event IS 'Datetime-typed chart events from MIMIC DATETIMEEVENTS, datetimeevents';

-- B7. DRG Code (diagnosis-related group)
CREATE TABLE IF NOT EXISTS cdr.c_drg_code (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    drg_type            VARCHAR(16),
    drg_code            VARCHAR(16),
    description         VARCHAR(512),
    drg_severity        INT,
    drg_mortality       INT,
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    source_id           VARCHAR(64),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_c_drg_encounter ON cdr.c_drg_code (encounter_id);

COMMENT ON TABLE cdr.c_drg_code IS 'DRG (Diagnosis Related Group) billing codes from MIMIC DRGCODES, drgcodes';

-- B8. CPT/HCPCS Event
CREATE TABLE IF NOT EXISTS cdr.c_cpt_event (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    chart_date          TIMESTAMP,
    cpt_code            VARCHAR(16),
    section_header      VARCHAR(128),
    subsection_header   VARCHAR(128),
    description         VARCHAR(512),
    cost_center         VARCHAR(16),
    seq_num             INT,
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    source_id           VARCHAR(64),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_c_cpt_encounter ON cdr.c_cpt_event (encounter_id);

COMMENT ON TABLE cdr.c_cpt_event IS 'CPT/HCPCS procedure billing codes from MIMIC CPTEVENTS, hcpcsevents';

-- B9. Caregiver (healthcare provider directory)
CREATE TABLE IF NOT EXISTS cdr.c_caregiver (
    id                  BIGSERIAL       PRIMARY KEY,
    caregiver_code      VARCHAR(32)     NOT NULL,
    label               VARCHAR(64),
    description         VARCHAR(128),
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    source_id           VARCHAR(64),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_c_caregiver_code ON cdr.c_caregiver (caregiver_code);

COMMENT ON TABLE cdr.c_caregiver IS 'Healthcare provider/caregiver directory from MIMIC CAREGIVERS, caregiver, provider';

-- B10. Pharmacy Order (MIMIC-IV specific)
CREATE TABLE IF NOT EXISTS cdr.c_pharmacy_order (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    pharmacy_id         BIGINT,
    poe_id              VARCHAR(32),
    medication          VARCHAR(256),
    proc_type           VARCHAR(32),
    status              VARCHAR(32),
    start_time          TIMESTAMP,
    end_time            TIMESTAMP,
    route               VARCHAR(32),
    frequency           VARCHAR(32),
    infusion_type       VARCHAR(32),
    doses_per_24_hrs    VARCHAR(32),
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    source_id           VARCHAR(64),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_c_pharm_encounter ON cdr.c_pharmacy_order (encounter_id);
CREATE INDEX idx_c_pharm_patient ON cdr.c_pharmacy_order (patient_id);

COMMENT ON TABLE cdr.c_pharmacy_order IS 'Pharmacy dispensing records from MIMIC-IV pharmacy';

-- B11. Provider Order (MIMIC-IV specific)
CREATE TABLE IF NOT EXISTS cdr.c_provider_order (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    poe_id              VARCHAR(32)     NOT NULL,
    poe_seq             INT,
    order_time          TIMESTAMP,
    order_type          VARCHAR(32),
    order_subtype       VARCHAR(64),
    transaction_type    VARCHAR(32),
    discontinue_of_poe_id VARCHAR(32),
    discontinued_by_poe_id VARCHAR(32),
    order_provider_id   VARCHAR(32),
    order_status        VARCHAR(32),
    order_detail        JSONB,                  -- from poe_detail key-value pairs
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    source_id           VARCHAR(64),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_c_poe_encounter ON cdr.c_provider_order (encounter_id);
CREATE INDEX idx_c_poe_poeid ON cdr.c_provider_order (poe_id);

COMMENT ON TABLE cdr.c_provider_order IS 'Provider Order Entry records from MIMIC-IV poe + poe_detail';

-- B12. Medication Administration Record (MIMIC-IV specific)
CREATE TABLE IF NOT EXISTS cdr.c_med_admin (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    emar_id             VARCHAR(32),
    poe_id              VARCHAR(32),
    pharmacy_id         BIGINT,
    chart_time          TIMESTAMP,
    medication          VARCHAR(256),
    event_txt           VARCHAR(128),
    schedule_time       TIMESTAMP,
    dose_given          VARCHAR(32),
    dose_given_unit     VARCHAR(32),
    route               VARCHAR(32),
    infusion_rate       VARCHAR(32),
    site                VARCHAR(32),
    detail              JSONB,                  -- from emar_detail key-value pairs
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    source_id           VARCHAR(64),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_c_ma_encounter ON cdr.c_med_admin (encounter_id);
CREATE INDEX idx_c_ma_pharmacy ON cdr.c_med_admin (pharmacy_id);

COMMENT ON TABLE cdr.c_med_admin IS 'Medication Administration Records from MIMIC-IV emar + emar_detail';

-- ===========================================================
-- Part C: New CDR dictionary/reference tables (6 tables)
-- ===========================================================

-- C1. Chart Item Dictionary
CREATE TABLE IF NOT EXISTS cdr.c_dict_item (
    id                  BIGSERIAL       PRIMARY KEY,
    itemid              INT             NOT NULL,
    label               VARCHAR(256),
    abbreviation        VARCHAR(64),
    category            VARCHAR(64),
    unit_name           VARCHAR(64),
    param_type          VARCHAR(32),
    dbsource            VARCHAR(32),
    linksto             VARCHAR(64),
    low_normal_value    DOUBLE PRECISION,
    high_normal_value   DOUBLE PRECISION,
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL,
    UNIQUE (itemid, source_dataset, org_id)
);

COMMENT ON TABLE cdr.c_dict_item IS 'Chart item dictionary from MIMIC D_ITEMS / d_items';

-- C2. Lab Item Dictionary
CREATE TABLE IF NOT EXISTS cdr.c_dict_lab_item (
    id                  BIGSERIAL       PRIMARY KEY,
    itemid              INT             NOT NULL,
    label               VARCHAR(256),
    fluid               VARCHAR(64),
    category            VARCHAR(64),
    loinc_code          VARCHAR(32),
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL,
    UNIQUE (itemid, source_dataset, org_id)
);

COMMENT ON TABLE cdr.c_dict_lab_item IS 'Lab test item dictionary from MIMIC D_LABITEMS / d_labitems';

-- C3. ICD Diagnosis Dictionary
CREATE TABLE IF NOT EXISTS cdr.c_dict_icd_diagnosis (
    id                  BIGSERIAL       PRIMARY KEY,
    icd_code            VARCHAR(16)     NOT NULL,
    icd_version         INT             NOT NULL,
    short_title         VARCHAR(128),
    long_title          VARCHAR(512),
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL,
    UNIQUE (icd_code, icd_version, source_dataset, org_id)
);

COMMENT ON TABLE cdr.c_dict_icd_diagnosis IS 'ICD diagnosis code dictionary from MIMIC D_ICD_DIAGNOSES / d_icd_diagnoses';

-- C4. ICD Procedure Dictionary
CREATE TABLE IF NOT EXISTS cdr.c_dict_icd_procedure (
    id                  BIGSERIAL       PRIMARY KEY,
    icd_code            VARCHAR(16)     NOT NULL,
    icd_version         INT             NOT NULL,
    short_title         VARCHAR(128),
    long_title          VARCHAR(512),
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL,
    UNIQUE (icd_code, icd_version, source_dataset, org_id)
);

COMMENT ON TABLE cdr.c_dict_icd_procedure IS 'ICD procedure code dictionary from MIMIC D_ICD_PROCEDURES / d_icd_procedures';

-- C5. CPT/HCPCS Dictionary
CREATE TABLE IF NOT EXISTS cdr.c_dict_procedure_code (
    id                  BIGSERIAL       PRIMARY KEY,
    code                VARCHAR(16)     NOT NULL,
    code_type           VARCHAR(8)      NOT NULL CHECK (code_type IN ('CPT', 'HCPCS')),
    category            VARCHAR(64),
    section_range       VARCHAR(32),
    section_header      VARCHAR(128),
    subsection_range    VARCHAR(32),
    subsection_header   VARCHAR(128),
    short_description   VARCHAR(256),
    long_description    TEXT,
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL,
    UNIQUE (code, code_type, source_dataset, org_id)
);

COMMENT ON TABLE cdr.c_dict_procedure_code IS 'CPT/HCPCS procedure code dictionary from MIMIC D_CPT / d_hcpcs';

-- C6. Ingredient Event (IV medication ingredients)
CREATE TABLE IF NOT EXISTS cdr.c_ingredient_event (
    id                  BIGSERIAL       PRIMARY KEY,
    encounter_id        BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    icu_stay_id         BIGINT,
    itemid              INT,
    start_time          TIMESTAMP,
    end_time            TIMESTAMP,
    amount              DECIMAL(16,6),
    amount_uom          VARCHAR(32),
    rate                DECIMAL(16,6),
    rate_uom            VARCHAR(32),
    order_id            BIGINT,
    link_order_id       BIGINT,
    status              VARCHAR(32),
    caregiver_id        BIGINT,
    source_dataset      VARCHAR(8)      NOT NULL CHECK (source_dataset IN ('MIMIC3', 'MIMIC4')),
    source_id           VARCHAR(64),
    created_by          VARCHAR(64)     NOT NULL DEFAULT 'etl',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_c_ige_encounter ON cdr.c_ingredient_event (encounter_id);
CREATE INDEX idx_c_ige_patient ON cdr.c_ingredient_event (patient_id);

COMMENT ON TABLE cdr.c_ingredient_event IS 'IV ingredient events from MIMIC-IV ingredientevents';
