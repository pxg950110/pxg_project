-- =============================================================================
-- MAIDC - CDR ETL Pipeline Management DDL
-- File: 12-cdr-etl.sql
-- Description: ODS->CDR ETL 管道管理，包含管道定义、步骤、字段映射、执行记录
--              4 张核心表
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. ETL 管道
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cdr.r_etl_pipeline (
    id                  BIGSERIAL       PRIMARY KEY,
    pipeline_name       VARCHAR(128)    NOT NULL,
    source_id           BIGINT          NOT NULL,
    description         TEXT,
    engine_type         VARCHAR(16)     NOT NULL DEFAULT 'EMBULK'
                                            CHECK (engine_type IN ('EMBULK','SPARK','PYTHON')),
    status              VARCHAR(16)     NOT NULL DEFAULT 'DRAFT'
                                            CHECK (status IN ('DRAFT','ACTIVE','DISABLED')),
    sync_mode           VARCHAR(16)     NOT NULL DEFAULT 'MANUAL'
                                            CHECK (sync_mode IN ('MANUAL','INCREMENTAL','FULL')),
    cron_expression     VARCHAR(64),
    last_run_time       TIMESTAMP,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_etl_pipeline_source ON cdr.r_etl_pipeline(source_id) WHERE NOT is_deleted;

COMMENT ON TABLE cdr.r_etl_pipeline IS 'ETL管道 - ODS到CDR的数据管道定义';

-- ---------------------------------------------------------------------------
-- 2. 管道步骤
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cdr.r_etl_step (
    id                  BIGSERIAL       PRIMARY KEY,
    pipeline_id         BIGINT          NOT NULL,
    step_name           VARCHAR(128)    NOT NULL,
    step_order          INT             NOT NULL,
    step_type           VARCHAR(16)     NOT NULL DEFAULT 'ONE_TO_ONE'
                                            CHECK (step_type IN ('ONE_TO_ONE','ONE_TO_MANY','MANY_TO_ONE')),
    source_schema       VARCHAR(32),
    source_table        VARCHAR(128)    NOT NULL,
    target_schema       VARCHAR(32),
    target_table        VARCHAR(128)    NOT NULL,
    join_config         JSONB,
    filter_condition    TEXT,
    transform_config    JSONB,
    pre_sql             TEXT,
    post_sql            TEXT,
    on_error            VARCHAR(16)     NOT NULL DEFAULT 'ABORT'
                                            CHECK (on_error IN ('SKIP','RETRY','ABORT')),
    sync_mode           VARCHAR(16)     NOT NULL DEFAULT 'INCREMENTAL'
                                            CHECK (sync_mode IN ('FULL','INCREMENTAL')),
    last_sync_time      TIMESTAMP,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_etl_step_pipeline ON cdr.r_etl_step(pipeline_id) WHERE NOT is_deleted;

COMMENT ON TABLE cdr.r_etl_step IS 'ETL管道步骤 - 定义管道内每个数据同步步骤';

-- ---------------------------------------------------------------------------
-- 3. 字段映射
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cdr.r_etl_field_mapping (
    id                  BIGSERIAL       PRIMARY KEY,
    step_id             BIGINT          NOT NULL,
    source_column       VARCHAR(64),
    source_table_alias  VARCHAR(32),
    target_column       VARCHAR(64)     NOT NULL,
    transform_type      VARCHAR(16)     NOT NULL DEFAULT 'DIRECT'
                                            CHECK (transform_type IN ('DIRECT','MAP','EXPRESSION','CONSTANT','DATE_FMT','LOOKUP')),
    transform_expr      TEXT,
    default_value       TEXT,
    is_required         BOOLEAN         NOT NULL DEFAULT FALSE,
    sort_order          INT,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_etl_mapping_step ON cdr.r_etl_field_mapping(step_id) WHERE NOT is_deleted;

COMMENT ON TABLE cdr.r_etl_field_mapping IS 'ETL字段映射 - 步骤内源字段到目标字段的映射规则';

-- ---------------------------------------------------------------------------
-- 4. 执行记录
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cdr.r_etl_execution (
    id                  BIGSERIAL       PRIMARY KEY,
    pipeline_id         BIGINT          NOT NULL,
    step_id             BIGINT,
    status              VARCHAR(16)     NOT NULL DEFAULT 'PENDING'
                                            CHECK (status IN ('PENDING','RUNNING','SUCCESS','FAILED','CANCELLED','SKIPPED')),
    engine_config       TEXT,
    start_time          TIMESTAMP,
    end_time            TIMESTAMP,
    rows_read           BIGINT          NOT NULL DEFAULT 0,
    rows_written        BIGINT          NOT NULL DEFAULT 0,
    rows_skipped        BIGINT          NOT NULL DEFAULT 0,
    rows_error          BIGINT          NOT NULL DEFAULT 0,
    error_message       TEXT,
    log_path            VARCHAR(256),
    trigger_type        VARCHAR(16)     NOT NULL DEFAULT 'MANUAL'
                                            CHECK (trigger_type IN ('MANUAL','SCHEDULE','RETRY')),
    execution_snapshot  JSONB,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_etl_exec_pipeline ON cdr.r_etl_execution(pipeline_id) WHERE NOT is_deleted;
CREATE INDEX idx_etl_exec_status   ON cdr.r_etl_execution(status) WHERE NOT is_deleted;
CREATE INDEX idx_etl_exec_time     ON cdr.r_etl_execution(created_at DESC) WHERE NOT is_deleted;

COMMENT ON TABLE cdr.r_etl_execution IS 'ETL执行记录 - 管道和步骤的执行历史与状态跟踪';
