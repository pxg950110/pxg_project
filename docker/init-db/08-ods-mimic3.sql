-- ============================================================
-- ODS MIMIC-III Tables (26 tables, prefix: o3_)
-- Source: PhysioNet MIMIC-III Clinical Database v1.4
-- ============================================================

-- -----------------------------------------------------------
-- Dimension / Dictionary Tables (small, no partitioning)
-- -----------------------------------------------------------

-- 1. Caregivers
CREATE TABLE IF NOT EXISTS ods.o3_caregivers (
    id              BIGSERIAL   PRIMARY KEY,
    row_id          INT,
    cgid            INT         NOT NULL,
    label           VARCHAR(64),
    description     VARCHAR(128),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);

-- 2. D_ITEMS (chart item dictionary)
CREATE TABLE IF NOT EXISTS ods.o3_d_items (
    id              BIGSERIAL   PRIMARY KEY,
    row_id          INT,
    itemid          INT         NOT NULL,
    label           VARCHAR(256),
    abbreviation    VARCHAR(64),
    dbsource        VARCHAR(32),
    linksto         VARCHAR(64),
    category        VARCHAR(64),
    unitname        VARCHAR(64),
    param_type      VARCHAR(32),
    conceptid       INT,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);

-- 3. D_LABITEMS (lab item dictionary)
CREATE TABLE IF NOT EXISTS ods.o3_d_labitems (
    id              BIGSERIAL   PRIMARY KEY,
    row_id          INT,
    itemid          INT         NOT NULL,
    label           VARCHAR(256),
    fluid           VARCHAR(64),
    category        VARCHAR(64),
    loinc_code      VARCHAR(32),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);

-- 4. D_ICD_DIAGNOSES
CREATE TABLE IF NOT EXISTS ods.o3_d_icd_diagnoses (
    id              BIGSERIAL   PRIMARY KEY,
    row_id          INT,
    icd9_code       VARCHAR(16) NOT NULL,
    short_title     VARCHAR(128),
    long_title      VARCHAR(512),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);

-- 5. D_ICD_PROCEDURES
CREATE TABLE IF NOT EXISTS ods.o3_d_icd_procedures (
    id              BIGSERIAL   PRIMARY KEY,
    row_id          INT,
    icd9_code       VARCHAR(16) NOT NULL,
    short_title     VARCHAR(128),
    long_title      VARCHAR(512),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);

-- 6. D_CPT
CREATE TABLE IF NOT EXISTS ods.o3_d_cpt (
    id              BIGSERIAL   PRIMARY KEY,
    row_id          INT,
    category        INT,
    sectionrange    VARCHAR(32),
    sectionheader   VARCHAR(128),
    subsectionrange VARCHAR(32),
    subsectionheader VARCHAR(128),
    codesuffix      VARCHAR(8),
    mincodeinsubsection INT,
    maxcodeinsubsection INT,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);

-- -----------------------------------------------------------
-- Core Clinical Dimension Tables (medium, no partitioning)
-- -----------------------------------------------------------

-- 7. Patients
CREATE TABLE IF NOT EXISTS ods.o3_patients (
    id              BIGSERIAL   PRIMARY KEY,
    row_id          INT,
    subject_id      INT         NOT NULL,
    gender          CHAR(1),
    dob             TIMESTAMP,
    dod             TIMESTAMP,
    dod_hosp        TIMESTAMP,
    dod_ssn         TIMESTAMP,
    expire_flag     INT,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o3_patients_subject ON ods.o3_patients (subject_id);

-- 8. Admissions
CREATE TABLE IF NOT EXISTS ods.o3_admissions (
    id                  BIGSERIAL   PRIMARY KEY,
    row_id              INT,
    subject_id          INT         NOT NULL,
    hadm_id             INT         NOT NULL,
    admittime           TIMESTAMP,
    dischtime           TIMESTAMP,
    deathtime           TIMESTAMP,
    admission_type      VARCHAR(32),
    admission_location  VARCHAR(64),
    discharge_location  VARCHAR(64),
    insurance           VARCHAR(32),
    language             VARCHAR(32),
    religion            VARCHAR(32),
    marital_status      VARCHAR(32),
    ethnicity           VARCHAR(128),
    edregtime           TIMESTAMP,
    edouttime           TIMESTAMP,
    diagnosis           VARCHAR(256),
    hospital_expire_flag INT,
    has_chartevents_data INT,
    _batch_id           VARCHAR(32) NOT NULL,
    _source_file        VARCHAR(128),
    _loaded_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash           VARCHAR(16),
    _is_valid           BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o3_admissions_subject ON ods.o3_admissions (subject_id);
CREATE INDEX idx_o3_admissions_hadm ON ods.o3_admissions (hadm_id);
CREATE INDEX idx_o3_admissions_admittime ON ods.o3_admissions (admittime);

-- 9. ICU Stays
CREATE TABLE IF NOT EXISTS ods.o3_icustays (
    id              BIGSERIAL   PRIMARY KEY,
    row_id          INT,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    icustay_id      INT         NOT NULL,
    dbsource        VARCHAR(32),
    first_careunit  VARCHAR(32),
    last_careunit   VARCHAR(32),
    first_wardid    INT,
    last_wardid     INT,
    intime          TIMESTAMP,
    outtime         TIMESTAMP,
    los             DECIMAL(16,6),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o3_icustays_subject ON ods.o3_icustays (subject_id);
CREATE INDEX idx_o3_icustays_hadm ON ods.o3_icustays (hadm_id);
CREATE INDEX idx_o3_icustays_icustay ON ods.o3_icustays (icustay_id);

-- 10. Services
CREATE TABLE IF NOT EXISTS ods.o3_services (
    id              BIGSERIAL   PRIMARY KEY,
    row_id          INT,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    transfertime    TIMESTAMP,
    prev_service    VARCHAR(32),
    curr_service    VARCHAR(32),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o3_services_subject ON ods.o3_services (subject_id, hadm_id);

-- 11. Transfers
CREATE TABLE IF NOT EXISTS ods.o3_transfers (
    id              BIGSERIAL   PRIMARY KEY,
    row_id          INT,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    icustay_id      INT,
    dbsource        VARCHAR(32),
    eventtype       VARCHAR(32),
    prev_careunit   VARCHAR(32),
    curr_careunit   VARCHAR(32),
    prev_wardid     INT,
    curr_wardid     INT,
    intime          TIMESTAMP,
    outtime         TIMESTAMP,
    los             DECIMAL(16,6),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o3_transfers_subject ON ods.o3_transfers (subject_id, hadm_id);

-- 12. Diagnoses ICD
CREATE TABLE IF NOT EXISTS ods.o3_diagnoses_icd (
    id              BIGSERIAL   PRIMARY KEY,
    row_id          INT,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    seq_num         INT,
    icd9_code       VARCHAR(16),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o3_diag_subject ON ods.o3_diagnoses_icd (subject_id, hadm_id);

-- 13. Procedures ICD
CREATE TABLE IF NOT EXISTS ods.o3_procedures_icd (
    id              BIGSERIAL   PRIMARY KEY,
    row_id          INT,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    seq_num         INT,
    icd9_code       VARCHAR(16),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o3_proc_icd_subject ON ods.o3_procedures_icd (subject_id, hadm_id);

-- 14. DRG Codes
CREATE TABLE IF NOT EXISTS ods.o3_drgcodes (
    id              BIGSERIAL   PRIMARY KEY,
    row_id          INT,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    drg_type        VARCHAR(16),
    drg_code        VARCHAR(16),
    description     VARCHAR(512),
    drg_severity    INT,
    drg_mortality   INT,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o3_drg_subject ON ods.o3_drgcodes (subject_id, hadm_id);

-- 15. CPT Events
CREATE TABLE IF NOT EXISTS ods.o3_cptevents (
    id              BIGSERIAL   PRIMARY KEY,
    row_id          INT,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    costcenter      VARCHAR(16),
    chartdate       TIMESTAMP,
    cpt_cd          VARCHAR(16),
    cpt_number      INT,
    cpt_suffix      VARCHAR(8),
    ticket_id_seq   INT,
    sectionheader   VARCHAR(128),
    subsectionheader VARCHAR(128),
    description     VARCHAR(512),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o3_cpt_subject ON ods.o3_cptevents (subject_id, hadm_id);

-- 16. Callout
CREATE TABLE IF NOT EXISTS ods.o3_callout (
    id                      BIGSERIAL   PRIMARY KEY,
    row_id                  INT,
    subject_id              INT         NOT NULL,
    hadm_id                 INT         NOT NULL,
    submit_wardid           INT,
    submit_careunit         VARCHAR(32),
    curr_wardid             INT,
    curr_careunit           VARCHAR(32),
    callout_wardid          INT,
    callout_service         VARCHAR(32),
    request_tele            INT,
    request_resp            INT,
    request_cdiff           INT,
    request_mrsa            INT,
    request_vre             INT,
    callout_status          VARCHAR(32),
    callout_outcome         VARCHAR(32),
    discharge_wardid        INT,
    acknowledge_status      VARCHAR(32),
    createtime              TIMESTAMP,
    updatetime              TIMESTAMP,
    acknowledgetime         TIMESTAMP,
    outcometime             TIMESTAMP,
    firstreservationtime    TIMESTAMP,
    currentreservationtime  TIMESTAMP,
    _batch_id               VARCHAR(32) NOT NULL,
    _source_file            VARCHAR(128),
    _loaded_at              TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash               VARCHAR(16),
    _is_valid               BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o3_callout_subject ON ods.o3_callout (subject_id, hadm_id);

-- -----------------------------------------------------------
-- Large Event Tables (partitioned by time)
-- -----------------------------------------------------------

-- 17. CHARTEVENTS (~33 GB) -- PARTITION BY RANGE (charttime)
CREATE TABLE IF NOT EXISTS ods.o3_chartevents (
    id              BIGSERIAL,
    row_id          INT,
    subject_id      INT         NOT NULL,
    hadm_id         INT,
    icustay_id      INT,
    itemid          INT         NOT NULL,
    charttime       TIMESTAMP,
    storetime       TIMESTAMP,
    cgid            INT,
    value_text      TEXT,
    valuenum        DOUBLE PRECISION,
    valueuom        VARCHAR(32),
    warning         INT,
    error           INT,
    resultstatus    VARCHAR(32),
    stopped         VARCHAR(32),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, charttime)
) PARTITION BY RANGE (charttime);

SELECT ods.create_monthly_partitions('o3_chartevents');
CREATE INDEX idx_o3_ce_subject ON ods.o3_chartevents (subject_id, hadm_id);
CREATE INDEX idx_o3_ce_itemid ON ods.o3_chartevents (itemid);

-- 18. LABEVENTS (~1.8 GB) -- PARTITION BY RANGE (charttime)
CREATE TABLE IF NOT EXISTS ods.o3_labevents (
    id              BIGSERIAL,
    row_id          INT,
    subject_id      INT         NOT NULL,
    hadm_id         INT,
    itemid          INT         NOT NULL,
    charttime       TIMESTAMP,
    value_text      TEXT,
    valuenum        DOUBLE PRECISION,
    valueuom        VARCHAR(32),
    flag            VARCHAR(16),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, charttime)
) PARTITION BY RANGE (charttime);

SELECT ods.create_monthly_partitions('o3_labevents');
CREATE INDEX idx_o3_le_subject ON ods.o3_labevents (subject_id, hadm_id);
CREATE INDEX idx_o3_le_itemid ON ods.o3_labevents (itemid);

-- 19. PRESCRIPTIONS (~735 MB) -- PARTITION BY RANGE (startdate)
CREATE TABLE IF NOT EXISTS ods.o3_prescriptions (
    id              BIGSERIAL,
    row_id          INT,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    icustay_id      INT,
    startdate       TIMESTAMP,
    enddate         TIMESTAMP,
    drug_type       VARCHAR(16),
    drug            VARCHAR(256),
    drug_name_poe   VARCHAR(256),
    drug_name_generic VARCHAR(256),
    formulary_drug_cd VARCHAR(32),
    gsn             VARCHAR(32),
    ndc             VARCHAR(32),
    prod_strength   VARCHAR(128),
    dose_val_rx     VARCHAR(64),
    dose_unit_rx    VARCHAR(32),
    form_val_disp   VARCHAR(64),
    form_unit_disp  VARCHAR(32),
    route           VARCHAR(32),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, startdate)
) PARTITION BY RANGE (startdate);

SELECT ods.create_monthly_partitions('o3_prescriptions');
CREATE INDEX idx_o3_rx_subject ON ods.o3_prescriptions (subject_id, hadm_id);

-- 20. INPUTEVENTS_CV (~2.3 GB) -- PARTITION BY RANGE (charttime)
CREATE TABLE IF NOT EXISTS ods.o3_inputevents_cv (
    id              BIGSERIAL,
    row_id          INT,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    icustay_id      INT,
    charttime       TIMESTAMP,
    itemid          INT,
    value_text      TEXT,
    valueuom        VARCHAR(32),
    storetime       TIMESTAMP,
    cgid            INT,
    stopped         VARCHAR(32),
    newbottle       INT,
    iserror         INT,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, charttime)
) PARTITION BY RANGE (charttime);

SELECT ods.create_monthly_partitions('o3_inputevents_cv');
CREATE INDEX idx_o3_iecv_subject ON ods.o3_inputevents_cv (subject_id, hadm_id);

-- 21. INPUTEVENTS_MV (~931 MB) -- PARTITION BY RANGE (starttime)
CREATE TABLE IF NOT EXISTS ods.o3_inputevents_mv (
    id                          BIGSERIAL,
    row_id                      INT,
    subject_id                  INT         NOT NULL,
    hadm_id                     INT         NOT NULL,
    icustay_id                  INT,
    starttime                   TIMESTAMP,
    endtime                     TIMESTAMP,
    itemid                      INT,
    amount                      DOUBLE PRECISION,
    amountuom                   VARCHAR(32),
    rate                        DOUBLE PRECISION,
    rateuom                     VARCHAR(32),
    storetime                   TIMESTAMP,
    cgid                        INT,
    orderid                     INT,
    linkorderid                 INT,
    ordercategoryname           VARCHAR(64),
    secondaryordercategoryname  VARCHAR(64),
    ordercomponenttypedescription VARCHAR(128),
    ordercategorydescription    VARCHAR(128),
    patientweight               DOUBLE PRECISION,
    totalamount                 DOUBLE PRECISION,
    totalamountuom              VARCHAR(32),
    isopenbag                   INT,
    continueinnextdept          INT,
    cancelreason                INT,
    statusdescription           VARCHAR(32),
    comments_editedby           INT,
    comments_canceledby         INT,
    comments_date               TIMESTAMP,
    originalamount              DOUBLE PRECISION,
    originalrate                DOUBLE PRECISION,
    originalrateuom             VARCHAR(32),
    originalsite                VARCHAR(64),
    _batch_id                   VARCHAR(32) NOT NULL,
    _source_file                VARCHAR(128),
    _loaded_at                  TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash                   VARCHAR(16),
    _is_valid                   BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, starttime)
) PARTITION BY RANGE (starttime);

SELECT ods.create_monthly_partitions('o3_inputevents_mv');
CREATE INDEX idx_o3_iemv_subject ON ods.o3_inputevents_mv (subject_id, hadm_id);

-- 22. OUTPUTEVENTS (~396 MB)
CREATE TABLE IF NOT EXISTS ods.o3_outputevents (
    id              BIGSERIAL   PRIMARY KEY,
    row_id          INT,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    icustay_id      INT,
    charttime       TIMESTAMP,
    itemid          INT,
    amount          DOUBLE PRECISION,
    amountuom       VARCHAR(32),
    rate            DOUBLE PRECISION,
    rateuom         VARCHAR(32),
    storetime       TIMESTAMP,
    cgid            INT,
    orderid         INT,
    linkorderid     INT,
    stopped         VARCHAR(32),
    newbottle       INT,
    originalamount  DOUBLE PRECISION,
    originalamountuom VARCHAR(32),
    originalroute   VARCHAR(32),
    originalrate    DOUBLE PRECISION,
    originalrateuom VARCHAR(32),
    originalsite    VARCHAR(64),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o3_oe_subject ON ods.o3_outputevents (subject_id, hadm_id);

-- 23. NOTEEVENTS (~3.8 GB) -- PARTITION BY RANGE (chartdate)
CREATE TABLE IF NOT EXISTS ods.o3_noteevents (
    id              BIGSERIAL,
    row_id          INT,
    subject_id      INT         NOT NULL,
    hadm_id         INT,
    chartdate       TIMESTAMP,
    charttime       TIMESTAMP,
    storetime       TIMESTAMP,
    category        VARCHAR(64),
    description     VARCHAR(256),
    cgid            INT,
    iserror         INT,
    text_content    TEXT,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, chartdate)
) PARTITION BY RANGE (chartdate);

SELECT ods.create_monthly_partitions('o3_noteevents');
CREATE INDEX idx_o3_ne_subject ON ods.o3_noteevents (subject_id, hadm_id);
CREATE INDEX idx_o3_ne_category ON ods.o3_noteevents (category);

-- 24. MICROBIOLOGYEVENTS (~73 MB)
CREATE TABLE IF NOT EXISTS ods.o3_microbiologyevents (
    id                  BIGSERIAL   PRIMARY KEY,
    row_id              INT,
    subject_id          INT         NOT NULL,
    hadm_id             INT         NOT NULL,
    chartdate           TIMESTAMP,
    charttime           TIMESTAMP,
    spec_itemid         INT,
    spec_type_desc      VARCHAR(128),
    org_itemid          INT,
    org_name            VARCHAR(256),
    isolate_num         INT,
    ab_itemid           INT,
    ab_name             VARCHAR(64),
    dilution_text       VARCHAR(32),
    dilution_comparison VARCHAR(16),
    dilution_value      DOUBLE PRECISION,
    interpretation      VARCHAR(16),
    _batch_id           VARCHAR(32) NOT NULL,
    _source_file        VARCHAR(128),
    _loaded_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash           VARCHAR(16),
    _is_valid           BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o3_micro_subject ON ods.o3_microbiologyevents (subject_id, hadm_id);

-- 25. DATETIMEEVENTS (~526 MB) -- PARTITION BY RANGE (starttime)
CREATE TABLE IF NOT EXISTS ods.o3_datetimeevents (
    id                          BIGSERIAL,
    row_id                      INT,
    subject_id                  INT         NOT NULL,
    hadm_id                     INT         NOT NULL,
    icustay_id                  INT,
    starttime                   TIMESTAMP,
    endtime                     TIMESTAMP,
    itemid                      INT,
    value_text                  TEXT,
    valueuom                    VARCHAR(32),
    location                    VARCHAR(64),
    locationcategory            VARCHAR(64),
    storetime                   TIMESTAMP,
    cgid                        INT,
    orderid                     INT,
    linkorderid                 INT,
    ordercategoryname           VARCHAR(64),
    secondaryordercategoryname  VARCHAR(64),
    ordercategorydescription    VARCHAR(128),
    isopenbag                   INT,
    continueinnextdept          INT,
    cancelreason                INT,
    statusdescription           VARCHAR(32),
    comments_editedby           INT,
    comments_canceledby         INT,
    comments_date               TIMESTAMP,
    _batch_id                   VARCHAR(32) NOT NULL,
    _source_file                VARCHAR(128),
    _loaded_at                  TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash                   VARCHAR(16),
    _is_valid                   BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, starttime)
) PARTITION BY RANGE (starttime);

SELECT ods.create_monthly_partitions('o3_datetimeevents');
CREATE INDEX idx_o3_dte_subject ON ods.o3_datetimeevents (subject_id, hadm_id);

-- 26. PROCEDUREEVENTS_MV (~49 MB)
CREATE TABLE IF NOT EXISTS ods.o3_procedureevents_mv (
    id                          BIGSERIAL   PRIMARY KEY,
    row_id                      INT,
    subject_id                  INT         NOT NULL,
    hadm_id                     INT         NOT NULL,
    icustay_id                  INT,
    itemid                      INT,
    starttime                   TIMESTAMP,
    endtime                     TIMESTAMP,
    storetime                   TIMESTAMP,
    cgid                        INT,
    value_text                  TEXT,
    valueuom                    VARCHAR(32),
    location                    VARCHAR(64),
    locationcategory            VARCHAR(64),
    orderid                     INT,
    linkorderid                 INT,
    ordercategoryname           VARCHAR(64),
    secondaryordercategoryname  VARCHAR(64),
    ordercategorydescription    VARCHAR(128),
    isopenbag                   INT,
    continueinnextdept          INT,
    cancelreason                INT,
    statusdescription           VARCHAR(32),
    comments_editedby           INT,
    comments_canceledby         INT,
    comments_date               TIMESTAMP,
    _batch_id                   VARCHAR(32) NOT NULL,
    _source_file                VARCHAR(128),
    _loaded_at                  TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash                   VARCHAR(16),
    _is_valid                   BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o3_pemv_subject ON ods.o3_procedureevents_mv (subject_id, hadm_id);

-- -----------------------------------------------------------
-- Batch tracking index (uniform across all ODS tables)
-- -----------------------------------------------------------
CREATE INDEX idx_o3_caregivers_batch ON ods.o3_caregivers (_batch_id);
CREATE INDEX idx_o3_d_items_batch ON ods.o3_d_items (_batch_id);
CREATE INDEX idx_o3_d_labitems_batch ON ods.o3_d_labitems (_batch_id);
CREATE INDEX idx_o3_d_icd_diag_batch ON ods.o3_d_icd_diagnoses (_batch_id);
CREATE INDEX idx_o3_d_icd_proc_batch ON ods.o3_d_icd_procedures (_batch_id);
CREATE INDEX idx_o3_d_cpt_batch ON ods.o3_d_cpt (_batch_id);
CREATE INDEX idx_o3_patients_batch ON ods.o3_patients (_batch_id);
CREATE INDEX idx_o3_admissions_batch ON ods.o3_admissions (_batch_id);
CREATE INDEX idx_o3_icustays_batch ON ods.o3_icustays (_batch_id);
CREATE INDEX idx_o3_services_batch ON ods.o3_services (_batch_id);
CREATE INDEX idx_o3_transfers_batch ON ods.o3_transfers (_batch_id);
CREATE INDEX idx_o3_diag_batch ON ods.o3_diagnoses_icd (_batch_id);
CREATE INDEX idx_o3_proc_icd_batch ON ods.o3_procedures_icd (_batch_id);
CREATE INDEX idx_o3_drg_batch ON ods.o3_drgcodes (_batch_id);
CREATE INDEX idx_o3_cpt_batch ON ods.o3_cptevents (_batch_id);
CREATE INDEX idx_o3_callout_batch ON ods.o3_callout (_batch_id);
CREATE INDEX idx_o3_oe_batch ON ods.o3_outputevents (_batch_id);
CREATE INDEX idx_o3_micro_batch ON ods.o3_microbiologyevents (_batch_id);
CREATE INDEX idx_o3_pemv_batch ON ods.o3_procedureevents_mv (_batch_id);
