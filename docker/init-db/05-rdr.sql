-- =============================================================================
-- MAIDC - Research Data Repository (RDR) Schema
-- File: 05-rdr.sql
-- Description: 科研数据仓库模块，包含科研项目、数据集、影像、基因组、
--              文本、ETL、数据质量等 19 张核心表
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. 研究项目
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_study_project (
    id                  BIGSERIAL       PRIMARY KEY,
    project_code        VARCHAR(32)     NOT NULL,
    project_name        VARCHAR(128)    NOT NULL,
    description         TEXT,
    research_type       VARCHAR(32)     NOT NULL
                                            CHECK (research_type IN ('CLINICAL_TRIAL','OBSERVATIONAL','GENOMIC','AI')),
    principal_investigator VARCHAR(64),
    start_date          DATE,
    end_date            DATE,
    status              VARCHAR(16)     NOT NULL DEFAULT 'DRAFT'
                                            CHECK (status IN ('DRAFT','APPROVED','ACTIVE','SUSPENDED','COMPLETED')),
    ethics_approval     VARCHAR(64),
    funding_source      VARCHAR(128),
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL,
    UNIQUE (project_code, org_id)
);

COMMENT ON TABLE rdr.r_study_project IS '研究项目';

-- ---------------------------------------------------------------------------
-- 2. 项目成员
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_study_member (
    id                  BIGSERIAL       PRIMARY KEY,
    project_id          BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    role                VARCHAR(32)     NOT NULL
                                            CHECK (role IN ('PI','CO_PI','RESEARCHER','DATA_MANAGER')),
    joined_at           TIMESTAMP,
    status              VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL,
    UNIQUE (project_id, user_id)
);

COMMENT ON TABLE rdr.r_study_member IS '项目成员';

-- ---------------------------------------------------------------------------
-- 3. 研究队列
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_research_cohort (
    id                  BIGSERIAL       PRIMARY KEY,
    project_id          BIGINT          NOT NULL,
    cohort_name         VARCHAR(128)    NOT NULL,
    description         TEXT,
    inclusion_criteria  JSONB,
    exclusion_criteria  JSONB,
    target_size         INT,
    current_size        INT             NOT NULL DEFAULT 0,
    status              VARCHAR(16)     NOT NULL DEFAULT 'DRAFT',
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_research_cohort IS '研究队列';

-- ---------------------------------------------------------------------------
-- 4. 研究受试者
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_study_subject (
    id                  BIGSERIAL       PRIMARY KEY,
    project_id          BIGINT          NOT NULL,
    cohort_id           BIGINT          NOT NULL,
    patient_id          BIGINT          NOT NULL,
    subject_no          VARCHAR(32),
    enrollment_date     DATE,
    withdrawal_date     DATE,
    status              VARCHAR(16)     NOT NULL DEFAULT 'ENROLLED'
                                            CHECK (status IN ('ENROLLED','WITHDRAWN','COMPLETED')),
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_r_study_subject_patient_id ON rdr.r_study_subject(patient_id);

COMMENT ON TABLE rdr.r_study_subject IS '研究受试者';

-- ---------------------------------------------------------------------------
-- 5. 数据集
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_dataset (
    id                  BIGSERIAL       PRIMARY KEY,
    project_id          BIGINT          NOT NULL,
    dataset_code        VARCHAR(32)     NOT NULL,
    dataset_name        VARCHAR(128)    NOT NULL,
    description         TEXT,
    dataset_type        VARCHAR(32)     NOT NULL
                                            CHECK (dataset_type IN ('CLINICAL','IMAGING','GENOMIC','TEXT','MULTIMODAL')),
    source_type         VARCHAR(32)     NOT NULL
                                            CHECK (source_type IN ('CDR','UPLOADED','GENERATED')),
    current_version     VARCHAR(16),
    total_records       BIGINT          NOT NULL DEFAULT 0,
    file_size           BIGINT          NOT NULL DEFAULT 0,
    storage_path        VARCHAR(256),
    status              VARCHAR(16)     NOT NULL DEFAULT 'DRAFT'
                                            CHECK (status IN ('DRAFT','ACTIVE','ARCHIVED','DELETED')),
    access_level        VARCHAR(16)     NOT NULL DEFAULT 'PROJECT'
                                            CHECK (access_level IN ('PUBLIC','PROJECT','RESTRICTED')),
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL,
    UNIQUE (dataset_code, org_id)
);

COMMENT ON TABLE rdr.r_dataset IS '数据集';

-- ---------------------------------------------------------------------------
-- 6. 数据集版本
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_dataset_version (
    id                  BIGSERIAL       PRIMARY KEY,
    dataset_id          BIGINT          NOT NULL,
    version_no          VARCHAR(16)     NOT NULL,
    changelog           TEXT,
    file_path           VARCHAR(256),
    file_size           BIGINT,
    checksum            VARCHAR(64),
    record_count        BIGINT,
    status              VARCHAR(16)     NOT NULL DEFAULT 'CREATED',
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL,
    UNIQUE (dataset_id, version_no)
);

COMMENT ON TABLE rdr.r_dataset_version IS '数据集版本';

-- ---------------------------------------------------------------------------
-- 7. 数据集访问日志
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_dataset_access_log (
    id                  BIGSERIAL       PRIMARY KEY,
    dataset_id          BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    access_type         VARCHAR(32)     NOT NULL
                                            CHECK (access_type IN ('QUERY','EXPORT','DOWNLOAD')),
    purpose             TEXT,
    record_count        INT,
    accessed_at         TIMESTAMP       NOT NULL DEFAULT NOW(),
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_r_dataset_access_log_dataset_id ON rdr.r_dataset_access_log(dataset_id);

COMMENT ON TABLE rdr.r_dataset_access_log IS '数据集访问日志';

-- ---------------------------------------------------------------------------
-- 8. 临床特征
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_clinical_feature (
    id                  BIGSERIAL       PRIMARY KEY,
    project_id          BIGINT          NOT NULL,
    feature_code        VARCHAR(64)     NOT NULL,
    feature_name        VARCHAR(128)    NOT NULL,
    data_type           VARCHAR(32)     NOT NULL
                                            CHECK (data_type IN ('NUMERIC','CATEGORICAL','DATETIME','TEXT','BOOLEAN')),
    source_table        VARCHAR(64),
    source_column       VARCHAR(64),
    unit                VARCHAR(32),
    value_range         JSONB,
    description         TEXT,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_clinical_feature IS '临床特征';

-- ---------------------------------------------------------------------------
-- 9. 特征字典
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_feature_dictionary (
    id                  BIGSERIAL       PRIMARY KEY,
    feature_name        VARCHAR(128)    NOT NULL,
    feature_category    VARCHAR(32)     NOT NULL,
    data_type           VARCHAR(32)     NOT NULL,
    standard_code       VARCHAR(64),
    standard_system     VARCHAR(32)
                            CHECK (standard_system IN ('ICD10','LOINC','SNOMED')),
    description         TEXT,
    is_enabled          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_r_feature_dictionary_category ON rdr.r_feature_dictionary(feature_category);

COMMENT ON TABLE rdr.r_feature_dictionary IS '特征字典';

-- ---------------------------------------------------------------------------
-- 10. 影像数据集
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_imaging_dataset (
    id                  BIGSERIAL       PRIMARY KEY,
    dataset_id          BIGINT          NOT NULL,
    exam_id             BIGINT,
    image_format        VARCHAR(16)     NOT NULL
                                            CHECK (image_format IN ('DICOM','PNG','JPEG','NIFTI')),
    resolution          JSONB,
    body_part           VARCHAR(64),
    annotation_status   VARCHAR(16)     NOT NULL DEFAULT 'NONE',
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_imaging_dataset IS '影像数据集';

-- ---------------------------------------------------------------------------
-- 11. 影像标注
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_imaging_annotation (
    id                  BIGSERIAL       PRIMARY KEY,
    imaging_dataset_id  BIGINT          NOT NULL,
    annotator_id        BIGINT          NOT NULL,
    annotation_type     VARCHAR(32)     NOT NULL
                                            CHECK (annotation_type IN ('BOUNDING_BOX','SEGMENTATION','LANDMARK','POLYGON')),
    annotation_data     JSONB,
    status              VARCHAR(16)     NOT NULL DEFAULT 'DRAFT'
                                            CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','REJECTED')),
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_imaging_annotation IS '影像标注';

-- ---------------------------------------------------------------------------
-- 12. 基因组数据集
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_genomic_dataset (
    id                  BIGSERIAL       PRIMARY KEY,
    dataset_id          BIGINT          NOT NULL,
    sample_id           VARCHAR(64)     NOT NULL,
    genome_build        VARCHAR(16)     NOT NULL
                                            CHECK (genome_build IN ('GRCh37','GRCh38')),
    file_format         VARCHAR(16)     NOT NULL
                                            CHECK (file_format IN ('VCF','BAM','FASTQ')),
    file_path           VARCHAR(256),
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_genomic_dataset IS '基因组数据集';

-- ---------------------------------------------------------------------------
-- 13. 基因组变异
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_genomic_variant (
    id                  BIGSERIAL       PRIMARY KEY,
    genomic_dataset_id  BIGINT          NOT NULL,
    chromosome          VARCHAR(16)     NOT NULL,
    position            INT             NOT NULL,
    ref_allele          VARCHAR(64),
    alt_allele          VARCHAR(64),
    variant_type        VARCHAR(32)     NOT NULL
                                            CHECK (variant_type IN ('SNP','INDEL','CNV')),
    gene_symbol         VARCHAR(32),
    significance        VARCHAR(32)
                            CHECK (significance IN ('PATHOGENIC','LIKELY_PATHOGENIC','BENIGN','VOUS')),
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_r_genomic_variant_gene_symbol ON rdr.r_genomic_variant(gene_symbol);

COMMENT ON TABLE rdr.r_genomic_variant IS '基因组变异';

-- ---------------------------------------------------------------------------
-- 14. 文本数据集
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_text_dataset (
    id                  BIGSERIAL       PRIMARY KEY,
    dataset_id          BIGINT          NOT NULL,
    source_type         VARCHAR(32)     NOT NULL
                                            CHECK (source_type IN ('CLINICAL_NOTE','LITERATURE','OTHER')),
    text_content        TEXT,
    language            VARCHAR(8)      NOT NULL DEFAULT 'zh',
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_text_dataset IS '文本数据集';

-- ---------------------------------------------------------------------------
-- 15. 文本标注
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_text_annotation (
    id                  BIGSERIAL       PRIMARY KEY,
    text_dataset_id     BIGINT          NOT NULL,
    annotator_id        BIGINT          NOT NULL,
    annotation_type     VARCHAR(32)     NOT NULL
                                            CHECK (annotation_type IN ('NER','RELATION','CLASSIFICATION')),
    entities            JSONB,
    status              VARCHAR(16)     NOT NULL DEFAULT 'DRAFT',
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_text_annotation IS '文本标注';

-- ---------------------------------------------------------------------------
-- 16. ETL 任务
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_etl_task (
    id                  BIGSERIAL       PRIMARY KEY,
    project_id          BIGINT          NOT NULL,
    task_name           VARCHAR(128)    NOT NULL,
    source_config       JSONB,
    target_config       JSONB,
    transform_config    JSONB,
    schedule_cron       VARCHAR(64),
    status              VARCHAR(16)     NOT NULL DEFAULT 'CREATED'
                                            CHECK (status IN ('CREATED','RUNNING','PAUSED','COMPLETED','FAILED')),
    last_run_at         TIMESTAMP,
    next_run_at         TIMESTAMP,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_etl_task IS 'ETL任务';

-- ---------------------------------------------------------------------------
-- 17. ETL 任务日志
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_etl_task_log (
    id                  BIGSERIAL       PRIMARY KEY,
    task_id             BIGINT          NOT NULL,
    run_no              INT             NOT NULL,
    start_time          TIMESTAMP,
    end_time            TIMESTAMP,
    status              VARCHAR(16)     NOT NULL
                                            CHECK (status IN ('RUNNING','SUCCESS','FAILED')),
    records_read        BIGINT,
    records_written     BIGINT,
    error_count         INT,
    error_message       TEXT,
    log_file_path       VARCHAR(256),
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_etl_task_log IS 'ETL任务日志';

-- ---------------------------------------------------------------------------
-- 18. 数据质量规则
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_data_quality_rule (
    id                  BIGSERIAL       PRIMARY KEY,
    project_id          BIGINT          NOT NULL,
    rule_name           VARCHAR(128)    NOT NULL,
    rule_type           VARCHAR(32)     NOT NULL
                                            CHECK (rule_type IN ('COMPLETENESS','ACCURACY','CONSISTENCY','TIMELINESS','UNIQUENESS')),
    target_table        VARCHAR(64),
    target_column       VARCHAR(64),
    rule_expr           JSONB,
    threshold           DECIMAL(5,2)    NOT NULL DEFAULT 100,
    severity            VARCHAR(16)     NOT NULL
                                            CHECK (severity IN ('WARNING','ERROR')),
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_data_quality_rule IS '数据质量规则';

-- ---------------------------------------------------------------------------
-- 19. 数据质量检测结果
-- ---------------------------------------------------------------------------
CREATE TABLE rdr.r_data_quality_result (
    id                  BIGSERIAL       PRIMARY KEY,
    rule_id             BIGINT          NOT NULL,
    dataset_id          BIGINT          NOT NULL,
    total_records       BIGINT,
    passed_records      BIGINT,
    failed_records      BIGINT,
    pass_rate           DECIMAL(5,2),
    detail              JSONB,
    checked_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    org_id              BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_data_quality_result IS '数据质量检测结果';
