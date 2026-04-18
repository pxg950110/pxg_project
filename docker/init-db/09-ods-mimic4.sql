-- ============================================================
-- ODS MIMIC-IV Tables (31 tables, prefix: o4_)
-- Source: PhysioNet MIMIC-IV v3.1 (hosp + icu modules)
-- ============================================================

-- ===========================================================
-- hosp/ - Hospital-level dimension tables (small)
-- ===========================================================

-- 1. Patients
CREATE TABLE IF NOT EXISTS ods.o4_patients (
    id              BIGSERIAL   PRIMARY KEY,
    subject_id      INT         NOT NULL,
    gender          CHAR(1),
    anchor_age      INT,
    anchor_year     INT,
    anchor_year_group VARCHAR(32),
    dod             TIMESTAMP,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o4_patients_subject ON ods.o4_patients (subject_id);

-- 2. Admissions
CREATE TABLE IF NOT EXISTS ods.o4_admissions (
    id                  BIGSERIAL   PRIMARY KEY,
    subject_id          INT         NOT NULL,
    hadm_id             INT         NOT NULL,
    admittime           TIMESTAMP,
    dischtime           TIMESTAMP,
    deathtime           TIMESTAMP,
    admission_type      VARCHAR(32),
    admit_provider_id   VARCHAR(32),
    admission_location  VARCHAR(64),
    discharge_location  VARCHAR(64),
    insurance           VARCHAR(32),
    language             VARCHAR(32),
    marital_status      VARCHAR(32),
    race                VARCHAR(128),
    edregtime           TIMESTAMP,
    edouttime           TIMESTAMP,
    hospital_expire_flag INT,
    _batch_id           VARCHAR(32) NOT NULL,
    _source_file        VARCHAR(128),
    _loaded_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash           VARCHAR(16),
    _is_valid           BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o4_admissions_subject ON ods.o4_admissions (subject_id);
CREATE INDEX idx_o4_admissions_hadm ON ods.o4_admissions (hadm_id);
CREATE INDEX idx_o4_admissions_admittime ON ods.o4_admissions (admittime);

-- 3. Provider
CREATE TABLE IF NOT EXISTS ods.o4_provider (
    id              BIGSERIAL   PRIMARY KEY,
    provider_id     VARCHAR(32) NOT NULL,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);

-- ===========================================================
-- hosp/ - Dictionary tables
-- ===========================================================

-- 4. D_HCPCS
CREATE TABLE IF NOT EXISTS ods.o4_d_hcpcs (
    id              BIGSERIAL   PRIMARY KEY,
    code            VARCHAR(16) NOT NULL,
    category        VARCHAR(64),
    long_description TEXT,
    short_description VARCHAR(256),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);

-- 5. D_ICD_DIAGNOSES
CREATE TABLE IF NOT EXISTS ods.o4_d_icd_diagnoses (
    id              BIGSERIAL   PRIMARY KEY,
    icd_code        VARCHAR(16) NOT NULL,
    icd_version     INT,
    long_title      VARCHAR(512),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);

-- 6. D_ICD_PROCEDURES
CREATE TABLE IF NOT EXISTS ods.o4_d_icd_procedures (
    id              BIGSERIAL   PRIMARY KEY,
    icd_code        VARCHAR(16) NOT NULL,
    icd_version     INT,
    long_title      VARCHAR(512),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);

-- 7. D_LABITEMS
CREATE TABLE IF NOT EXISTS ods.o4_d_labitems (
    id              BIGSERIAL   PRIMARY KEY,
    itemid          INT         NOT NULL,
    label           VARCHAR(256),
    fluid           VARCHAR(64),
    category        VARCHAR(64),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);

-- ===========================================================
-- hosp/ - Clinical event tables (medium)
-- ===========================================================

-- 8. Diagnoses ICD
CREATE TABLE IF NOT EXISTS ods.o4_diagnoses_icd (
    id              BIGSERIAL   PRIMARY KEY,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    seq_num         INT,
    icd_code        VARCHAR(16),
    icd_version     INT,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o4_diag_subject ON ods.o4_diagnoses_icd (subject_id, hadm_id);

-- 9. DRG Codes
CREATE TABLE IF NOT EXISTS ods.o4_drgcodes (
    id              BIGSERIAL   PRIMARY KEY,
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
CREATE INDEX idx_o4_drg_subject ON ods.o4_drgcodes (subject_id, hadm_id);

-- 10. HCPCS Events
CREATE TABLE IF NOT EXISTS ods.o4_hcpcsevents (
    id              BIGSERIAL   PRIMARY KEY,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    chartdate       TIMESTAMP,
    hcpcs_cd        VARCHAR(16),
    seq_num         INT,
    short_description VARCHAR(256),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o4_hcpcs_subject ON ods.o4_hcpcsevents (subject_id, hadm_id);

-- 11. Procedures ICD
CREATE TABLE IF NOT EXISTS ods.o4_procedures_icd (
    id              BIGSERIAL   PRIMARY KEY,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    seq_num         INT,
    chartdate       TIMESTAMP,
    icd_code        VARCHAR(16),
    icd_version     INT,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o4_proc_icd_subject ON ods.o4_procedures_icd (subject_id, hadm_id);

-- 12. Services
CREATE TABLE IF NOT EXISTS ods.o4_services (
    id              BIGSERIAL   PRIMARY KEY,
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
CREATE INDEX idx_o4_services_subject ON ods.o4_services (subject_id, hadm_id);

-- 13. Transfers
CREATE TABLE IF NOT EXISTS ods.o4_transfers (
    id              BIGSERIAL   PRIMARY KEY,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    transfer_id     INT,
    eventtype       VARCHAR(32),
    careunit        VARCHAR(32),
    intime          TIMESTAMP,
    outtime         TIMESTAMP,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o4_transfers_subject ON ods.o4_transfers (subject_id, hadm_id);

-- 14. OMR (Outpatient Measurements)
CREATE TABLE IF NOT EXISTS ods.o4_omr (
    id              BIGSERIAL   PRIMARY KEY,
    subject_id      INT         NOT NULL,
    chartdate       TIMESTAMP,
    seq_num         INT,
    result_name     VARCHAR(128),
    result_value    TEXT,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o4_omr_subject ON ods.o4_omr (subject_id);

-- ===========================================================
-- hosp/ - Large event tables (partitioned)
-- ===========================================================

-- 15. LABEVENTS (~18 GB) -- PARTITION BY RANGE (charttime)
CREATE TABLE IF NOT EXISTS ods.o4_labevents (
    id              BIGSERIAL,
    labevent_id     BIGINT,
    subject_id      INT         NOT NULL,
    hadm_id         INT,
    specimen_id     BIGINT,
    itemid          INT         NOT NULL,
    order_provider_id VARCHAR(32),
    charttime       TIMESTAMP,
    storetime       TIMESTAMP,
    value_text      TEXT,
    valuenum        DOUBLE PRECISION,
    valueuom        VARCHAR(32),
    ref_range_lower DOUBLE PRECISION,
    ref_range_upper DOUBLE PRECISION,
    flag            VARCHAR(16),
    priority        VARCHAR(16),
    comments        TEXT,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, charttime)
) PARTITION BY RANGE (charttime);

SELECT ods.create_monthly_partitions('o4_labevents');
CREATE INDEX idx_o4_le_subject ON ods.o4_labevents (subject_id, hadm_id);
CREATE INDEX idx_o4_le_itemid ON ods.o4_labevents (itemid);

-- 16. MICROBIOLOGYEVENTS (~909 MB)
CREATE TABLE IF NOT EXISTS ods.o4_microbiologyevents (
    id              BIGSERIAL   PRIMARY KEY,
    microevent_id   BIGINT,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    micro_specimen_id BIGINT,
    order_provider_id VARCHAR(32),
    chartdate       TIMESTAMP,
    charttime       TIMESTAMP,
    spec_itemid     INT,
    spec_type_desc  VARCHAR(128),
    test_seq        INT,
    storedate       TIMESTAMP,
    storetime       TIMESTAMP,
    test_itemid     INT,
    test_name       VARCHAR(128),
    org_itemid      INT,
    org_name        VARCHAR(256),
    isolate_num     INT,
    quantity        VARCHAR(32),
    ab_itemid       INT,
    ab_name         VARCHAR(64),
    dilution_text   VARCHAR(32),
    dilution_comparison VARCHAR(16),
    dilution_value  DOUBLE PRECISION,
    interpretation  VARCHAR(16),
    comments        TEXT,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o4_micro_subject ON ods.o4_microbiologyevents (subject_id, hadm_id);

-- 17. PHARMACY (~4 GB) -- PARTITION BY RANGE (starttime)
CREATE TABLE IF NOT EXISTS ods.o4_pharmacy (
    id              BIGSERIAL,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    pharmacy_id     BIGINT,
    poe_id          VARCHAR(32),
    starttime       TIMESTAMP,
    stoptime        TIMESTAMP,
    medication      VARCHAR(256),
    proc_type       VARCHAR(32),
    status          VARCHAR(32),
    entertime       TIMESTAMP,
    verifiedtime    TIMESTAMP,
    route           VARCHAR(32),
    frequency       VARCHAR(32),
    disp_sched      VARCHAR(64),
    infusion_type   VARCHAR(32),
    sliding_scale   VARCHAR(32),
    lockout_interval VARCHAR(32),
    basal_rate      VARCHAR(32),
    one_hr_max      VARCHAR(32),
    doses_per_24_hrs VARCHAR(32),
    duration        VARCHAR(32),
    duration_interval VARCHAR(32),
    expiration_value VARCHAR(32),
    expiration_unit VARCHAR(32),
    expirationdate  TIMESTAMP,
    dispensation    VARCHAR(32),
    fill_quantity   VARCHAR(32),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, starttime)
) PARTITION BY RANGE (starttime);

SELECT ods.create_monthly_partitions('o4_pharmacy');
CREATE INDEX idx_o4_pharm_subject ON ods.o4_pharmacy (subject_id, hadm_id);

-- 18. POE (~5.1 GB) -- PARTITION BY RANGE (ordertime)
CREATE TABLE IF NOT EXISTS ods.o4_poe (
    id                  BIGSERIAL,
    poe_id              VARCHAR(32) NOT NULL,
    poe_seq             INT,
    subject_id          INT         NOT NULL,
    hadm_id             INT         NOT NULL,
    ordertime           TIMESTAMP,
    order_type          VARCHAR(32),
    order_subtype       VARCHAR(64),
    transaction_type    VARCHAR(32),
    discontinue_of_poe_id VARCHAR(32),
    discontinued_by_poe_id VARCHAR(32),
    order_provider_id   VARCHAR(32),
    order_status        VARCHAR(32),
    _batch_id           VARCHAR(32) NOT NULL,
    _source_file        VARCHAR(128),
    _loaded_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash           VARCHAR(16),
    _is_valid           BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, ordertime)
) PARTITION BY RANGE (ordertime);

SELECT ods.create_monthly_partitions('o4_poe');
CREATE INDEX idx_o4_poe_subject ON ods.o4_poe (subject_id, hadm_id);

-- 19. POE_DETAIL (~424 MB)
CREATE TABLE IF NOT EXISTS ods.o4_poe_detail (
    id              BIGSERIAL   PRIMARY KEY,
    poe_id          VARCHAR(32) NOT NULL,
    poe_seq         INT,
    subject_id      INT         NOT NULL,
    field_name      VARCHAR(64),
    field_value     TEXT,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o4_poed_poe ON ods.o4_poe_detail (poe_id);

-- 20. PRESCRIPTIONS (~3.5 GB) -- PARTITION BY RANGE (starttime)
CREATE TABLE IF NOT EXISTS ods.o4_prescriptions (
    id              BIGSERIAL,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    pharmacy_id     BIGINT,
    poe_id          VARCHAR(32),
    poe_seq         INT,
    order_provider_id VARCHAR(32),
    starttime       TIMESTAMP,
    stoptime        TIMESTAMP,
    drug_type       VARCHAR(16),
    drug            VARCHAR(256),
    formulary_drug_cd VARCHAR(32),
    gsn             VARCHAR(32),
    ndc             VARCHAR(32),
    prod_strength   VARCHAR(128),
    form_rx         VARCHAR(32),
    dose_val_rx     VARCHAR(64),
    dose_unit_rx    VARCHAR(32),
    form_val_disp   VARCHAR(64),
    form_unit_disp  VARCHAR(32),
    doses_per_24_hrs VARCHAR(32),
    route           VARCHAR(32),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, starttime)
) PARTITION BY RANGE (starttime);

SELECT ods.create_monthly_partitions('o4_prescriptions');
CREATE INDEX idx_o4_rx_subject ON ods.o4_prescriptions (subject_id, hadm_id);

-- 21. EMAR (~6.2 GB) -- PARTITION BY RANGE (charttime)
CREATE TABLE IF NOT EXISTS ods.o4_emar (
    id              BIGSERIAL,
    subject_id      INT         NOT NULL,
    hadm_id         INT,
    emar_id         VARCHAR(32),
    emar_seq        INT,
    poe_id          VARCHAR(32),
    pharmacy_id     BIGINT,
    charttime       TIMESTAMP,
    medication      VARCHAR(256),
    event_txt       VARCHAR(128),
    scheduletime    TIMESTAMP,
    storetime       TIMESTAMP,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, charttime)
) PARTITION BY RANGE (charttime);

SELECT ods.create_monthly_partitions('o4_emar');
CREATE INDEX idx_o4_emar_subject ON ods.o4_emar (subject_id, hadm_id);

-- 22. EMAR_DETAIL (~8.7 GB) -- PARTITION BY RANGE (charttime)
CREATE TABLE IF NOT EXISTS ods.o4_emar_detail (
    id              BIGSERIAL,
    subject_id      INT         NOT NULL,
    emar_id         VARCHAR(32),
    parent_field_ordinal INT,
    administration_type VARCHAR(32),
    pharmacy_id     BIGINT,
    barcode_type    VARCHAR(32),
    reason_for_no_barcode VARCHAR(256),
    complete_dose_not_given VARCHAR(256),
    dose_due        VARCHAR(32),
    dose_given      VARCHAR(32),
    dose_given_unit VARCHAR(32),
    will_remaining_dose_be_given VARCHAR(16),
    product_amount_given VARCHAR(32),
    product_unit    VARCHAR(32),
    product_code    VARCHAR(32),
    product_description VARCHAR(256),
    product_description_other VARCHAR(256),
    prior_infusion_rate VARCHAR(32),
    infusion_rate   VARCHAR(32),
    infusion_rate_adjustment VARCHAR(256),
    infusion_rate_unit VARCHAR(32),
    route           VARCHAR(32),
    infusion_complete VARCHAR(16),
    completion_interval VARCHAR(64),
    new_iv_bag_hung VARCHAR(16),
    continued_infusion_in_other_location VARCHAR(16),
    restart_interval VARCHAR(64),
    side            VARCHAR(32),
    site            VARCHAR(32),
    non_formulary_visual_verification VARCHAR(16),
    charttime       TIMESTAMP,
    medication      VARCHAR(256),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, charttime)
) PARTITION BY RANGE (charttime);

SELECT ods.create_monthly_partitions('o4_emar_detail');
CREATE INDEX idx_o4_emard_subject ON ods.o4_emar_detail (subject_id);

-- ===========================================================
-- icu/ - ICU dimension tables
-- ===========================================================

-- 23. Caregiver
CREATE TABLE IF NOT EXISTS ods.o4_caregiver (
    id              BIGSERIAL   PRIMARY KEY,
    caregiver_id    INT         NOT NULL,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);

-- 24. D_ITEMS (ICU chart item dictionary)
CREATE TABLE IF NOT EXISTS ods.o4_d_items (
    id              BIGSERIAL   PRIMARY KEY,
    itemid          INT         NOT NULL,
    label           VARCHAR(256),
    abbreviation    VARCHAR(64),
    linksto         VARCHAR(64),
    category        VARCHAR(64),
    unitname        VARCHAR(64),
    param_type      VARCHAR(32),
    lownormalvalue  DOUBLE PRECISION,
    highnormalvalue DOUBLE PRECISION,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);

-- 25. ICU Stays
CREATE TABLE IF NOT EXISTS ods.o4_icustays (
    id              BIGSERIAL   PRIMARY KEY,
    subject_id      INT         NOT NULL,
    hadm_id         INT         NOT NULL,
    stay_id         INT         NOT NULL,
    first_careunit  VARCHAR(32),
    last_careunit   VARCHAR(32),
    intime          TIMESTAMP,
    outtime         TIMESTAMP,
    los             DECIMAL(16,6),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o4_icustays_subject ON ods.o4_icustays (subject_id);
CREATE INDEX idx_o4_icustays_hadm ON ods.o4_icustays (hadm_id);
CREATE INDEX idx_o4_icustays_stay ON ods.o4_icustays (stay_id);

-- ===========================================================
-- icu/ - ICU large event tables (partitioned)
-- ===========================================================

-- 26. CHARTEVENTS (~42 GB) -- PARTITION BY RANGE (charttime)
CREATE TABLE IF NOT EXISTS ods.o4_chartevents (
    id              BIGSERIAL,
    subject_id      INT         NOT NULL,
    hadm_id         INT,
    stay_id         INT,
    caregiver_id    INT,
    charttime       TIMESTAMP,
    storetime       TIMESTAMP,
    itemid          INT         NOT NULL,
    value_text      TEXT,
    valuenum        DOUBLE PRECISION,
    valueuom        VARCHAR(32),
    warning         BOOLEAN,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, charttime)
) PARTITION BY RANGE (charttime);

SELECT ods.create_monthly_partitions('o4_chartevents');
CREATE INDEX idx_o4_ce_subject ON ods.o4_chartevents (subject_id, hadm_id);
CREATE INDEX idx_o4_ce_itemid ON ods.o4_chartevents (itemid);
CREATE INDEX idx_o4_ce_stay ON ods.o4_chartevents (stay_id);

-- 27. DATETIMEEVENTS (~1.1 GB) -- PARTITION BY RANGE (charttime)
CREATE TABLE IF NOT EXISTS ods.o4_datetimeevents (
    id              BIGSERIAL,
    subject_id      INT         NOT NULL,
    hadm_id         INT,
    stay_id         INT,
    caregiver_id    INT,
    charttime       TIMESTAMP,
    storetime       TIMESTAMP,
    itemid          INT         NOT NULL,
    value_text      TEXT,
    valueuom        VARCHAR(32),
    warning         BOOLEAN,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, charttime)
) PARTITION BY RANGE (charttime);

SELECT ods.create_monthly_partitions('o4_datetimeevents');
CREATE INDEX idx_o4_dte_subject ON ods.o4_datetimeevents (subject_id, hadm_id);

-- 28. INGREDIENTEVENTS (~2.5 GB) -- PARTITION BY RANGE (starttime)
CREATE TABLE IF NOT EXISTS ods.o4_ingredientevents (
    id              BIGSERIAL,
    subject_id      INT         NOT NULL,
    hadm_id         INT,
    stay_id         INT,
    caregiver_id    INT,
    starttime       TIMESTAMP,
    endtime         TIMESTAMP,
    storetime       TIMESTAMP,
    itemid          INT,
    amount          DOUBLE PRECISION,
    amountuom       VARCHAR(32),
    rate            DOUBLE PRECISION,
    rateuom         VARCHAR(32),
    orderid         INT,
    linkorderid     INT,
    statusdescription VARCHAR(32),
    originalamount  DOUBLE PRECISION,
    originalrate    DOUBLE PRECISION,
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, starttime)
) PARTITION BY RANGE (starttime);

SELECT ods.create_monthly_partitions('o4_ingredientevents');
CREATE INDEX idx_o4_ige_subject ON ods.o4_ingredientevents (subject_id, hadm_id);

-- 29. INPUTEVENTS (~2.9 GB) -- PARTITION BY RANGE (starttime)
CREATE TABLE IF NOT EXISTS ods.o4_inputevents (
    id                          BIGSERIAL,
    subject_id                  INT         NOT NULL,
    hadm_id                     INT,
    stay_id                     INT,
    caregiver_id                INT,
    starttime                   TIMESTAMP,
    endtime                     TIMESTAMP,
    storetime                   TIMESTAMP,
    itemid                      INT,
    amount                      DOUBLE PRECISION,
    amountuom                   VARCHAR(32),
    rate                        DOUBLE PRECISION,
    rateuom                     VARCHAR(32),
    orderid                     INT,
    linkorderid                 INT,
    ordercategoryname           VARCHAR(64),
    secondaryordercategoryname  VARCHAR(64),
    ordercomponenttypedescription VARCHAR(128),
    ordercategorydescription    VARCHAR(128),
    patientweight               DOUBLE PRECISION,
    totalamount                 DOUBLE PRECISION,
    totalamountuom              VARCHAR(32),
    isopenbag                   BOOLEAN,
    continueinnextdept          BOOLEAN,
    statusdescription           VARCHAR(32),
    originalamount              DOUBLE PRECISION,
    originalrate                DOUBLE PRECISION,
    _batch_id                   VARCHAR(32) NOT NULL,
    _source_file                VARCHAR(128),
    _loaded_at                  TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash                   VARCHAR(16),
    _is_valid                   BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id, starttime)
) PARTITION BY RANGE (starttime);

SELECT ods.create_monthly_partitions('o4_inputevents');
CREATE INDEX idx_o4_ie_subject ON ods.o4_inputevents (subject_id, hadm_id);

-- 30. OUTPUTEVENTS (~462 MB)
CREATE TABLE IF NOT EXISTS ods.o4_outputevents (
    id              BIGSERIAL   PRIMARY KEY,
    subject_id      INT         NOT NULL,
    hadm_id         INT,
    stay_id         INT,
    caregiver_id    INT,
    charttime       TIMESTAMP,
    storetime       TIMESTAMP,
    itemid          INT,
    value_num       DOUBLE PRECISION,
    valueuom        VARCHAR(32),
    _batch_id       VARCHAR(32) NOT NULL,
    _source_file    VARCHAR(128),
    _loaded_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash       VARCHAR(16),
    _is_valid       BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o4_oe_subject ON ods.o4_outputevents (subject_id, hadm_id);

-- 31. PROCEDUREEVENTS (~150 MB)
CREATE TABLE IF NOT EXISTS ods.o4_procedureevents (
    id                          BIGSERIAL   PRIMARY KEY,
    subject_id                  INT         NOT NULL,
    hadm_id                     INT,
    stay_id                     INT,
    caregiver_id                INT,
    starttime                   TIMESTAMP,
    endtime                     TIMESTAMP,
    storetime                   TIMESTAMP,
    itemid                      INT,
    value_text                  TEXT,
    valueuom                    VARCHAR(32),
    location                    VARCHAR(64),
    locationcategory            VARCHAR(64),
    orderid                     INT,
    linkorderid                 INT,
    ordercategoryname           VARCHAR(64),
    ordercategorydescription    VARCHAR(128),
    patientweight               DOUBLE PRECISION,
    isopenbag                   BOOLEAN,
    continueinnextdept          BOOLEAN,
    statusdescription           VARCHAR(32),
    originalamount              DOUBLE PRECISION,
    originalrate                DOUBLE PRECISION,
    _batch_id                   VARCHAR(32) NOT NULL,
    _source_file                VARCHAR(128),
    _loaded_at                  TIMESTAMP   NOT NULL DEFAULT NOW(),
    _row_hash                   VARCHAR(16),
    _is_valid                   BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_o4_pe_subject ON ods.o4_procedureevents (subject_id, hadm_id);

-- -----------------------------------------------------------
-- Batch tracking indexes (uniform across all ODS IV tables)
-- -----------------------------------------------------------
CREATE INDEX idx_o4_patients_batch ON ods.o4_patients (_batch_id);
CREATE INDEX idx_o4_admissions_batch ON ods.o4_admissions (_batch_id);
CREATE INDEX idx_o4_provider_batch ON ods.o4_provider (_batch_id);
CREATE INDEX idx_o4_d_hcpcs_batch ON ods.o4_d_hcpcs (_batch_id);
CREATE INDEX idx_o4_d_icd_diag_batch ON ods.o4_d_icd_diagnoses (_batch_id);
CREATE INDEX idx_o4_d_icd_proc_batch ON ods.o4_d_icd_procedures (_batch_id);
CREATE INDEX idx_o4_d_labitems_batch ON ods.o4_d_labitems (_batch_id);
CREATE INDEX idx_o4_diag_batch ON ods.o4_diagnoses_icd (_batch_id);
CREATE INDEX idx_o4_drg_batch ON ods.o4_drgcodes (_batch_id);
CREATE INDEX idx_o4_hcpcs_batch ON ods.o4_hcpcsevents (_batch_id);
CREATE INDEX idx_o4_proc_icd_batch ON ods.o4_procedures_icd (_batch_id);
CREATE INDEX idx_o4_services_batch ON ods.o4_services (_batch_id);
CREATE INDEX idx_o4_transfers_batch ON ods.o4_transfers (_batch_id);
CREATE INDEX idx_o4_omr_batch ON ods.o4_omr (_batch_id);
CREATE INDEX idx_o4_micro_batch ON ods.o4_microbiologyevents (_batch_id);
CREATE INDEX idx_o4_poed_batch ON ods.o4_poe_detail (_batch_id);
CREATE INDEX idx_o4_caregiver_batch ON ods.o4_caregiver (_batch_id);
CREATE INDEX idx_o4_d_items_batch ON ods.o4_d_items (_batch_id);
CREATE INDEX idx_o4_icustays_batch ON ods.o4_icustays (_batch_id);
CREATE INDEX idx_o4_oe_batch ON ods.o4_outputevents (_batch_id);
CREATE INDEX idx_o4_pe_batch ON ods.o4_procedureevents (_batch_id);
