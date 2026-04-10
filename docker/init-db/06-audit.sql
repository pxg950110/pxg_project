-- =============================================================================
-- MAIDC - Audit Schema
-- File: 06-audit.sql
-- Description: 审计日志模块，包含操作审计、数据访问审计、系统事件 3 张表
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. 操作审计日志
-- ---------------------------------------------------------------------------
CREATE TABLE audit.a_audit_log (
    id                  BIGSERIAL       PRIMARY KEY,
    trace_id            VARCHAR(64)     NOT NULL,
    user_id             BIGINT,
    username            VARCHAR(64),
    service_name        VARCHAR(64)     NOT NULL,
    operation           VARCHAR(32)     NOT NULL
                                            CHECK (operation IN ('CREATE','READ','UPDATE','DELETE','LOGIN','LOGOUT')),
    resource_type       VARCHAR(32)     NOT NULL,
    resource_id         VARCHAR(64),
    resource_name       VARCHAR(128),
    request_method      VARCHAR(8),
    request_url         VARCHAR(256),
    request_params      JSONB,
    response_code       INT,
    response_msg        VARCHAR(256),
    ip_address          VARCHAR(45),
    user_agent          VARCHAR(256),
    duration_ms         INT,
    status              VARCHAR(16)     NOT NULL
                                            CHECK (status IN ('SUCCESS','FAILURE')),
    error_message       TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_a_audit_log_trace_id ON audit.a_audit_log(trace_id);
CREATE INDEX idx_a_audit_log_user_created ON audit.a_audit_log(user_id, created_at);
CREATE INDEX idx_a_audit_log_resource ON audit.a_audit_log(resource_type, resource_id);
CREATE INDEX idx_a_audit_log_created_at ON audit.a_audit_log(created_at);

COMMENT ON TABLE audit.a_audit_log IS '操作审计日志';

-- ---------------------------------------------------------------------------
-- 2. 数据访问审计日志
-- ---------------------------------------------------------------------------
CREATE TABLE audit.a_data_access_log (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    access_type         VARCHAR(32)     NOT NULL
                                            CHECK (access_type IN ('QUERY','EXPORT','DOWNLOAD','SHARE')),
    data_domain         VARCHAR(32)     NOT NULL
                                            CHECK (data_domain IN ('CDR','RDR','MODEL')),
    table_name          VARCHAR(64),
    record_id           BIGINT,
    patient_id          BIGINT,
    purpose             TEXT,
    data_volume         BIGINT,
    ip_address          VARCHAR(45),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_a_data_access_log_user_created ON audit.a_data_access_log(user_id, created_at);
CREATE INDEX idx_a_data_access_log_patient_id ON audit.a_data_access_log(patient_id);

COMMENT ON TABLE audit.a_data_access_log IS '数据访问审计日志';

-- ---------------------------------------------------------------------------
-- 3. 系统事件
-- ---------------------------------------------------------------------------
CREATE TABLE audit.a_system_event (
    id                  BIGSERIAL       PRIMARY KEY,
    event_type          VARCHAR(32)     NOT NULL
                                            CHECK (event_type IN ('SERVICE_START','SERVICE_STOP','CONFIG_CHANGE','DEPLOY','ALERT')),
    event_level         VARCHAR(16)     NOT NULL
                                            CHECK (event_level IN ('INFO','WARN','ERROR','CRITICAL')),
    source              VARCHAR(64)     NOT NULL,
    event_title         VARCHAR(128)    NOT NULL,
    event_detail        TEXT,
    event_data          JSONB,
    resolved            BOOLEAN         NOT NULL DEFAULT FALSE,
    resolved_by         VARCHAR(64),
    resolved_at         TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    org_id              BIGINT          NOT NULL
);

CREATE INDEX idx_a_system_event_type_created ON audit.a_system_event(event_type, created_at);
CREATE INDEX idx_a_system_event_level_created ON audit.a_system_event(event_level, created_at);

COMMENT ON TABLE audit.a_system_event IS '系统事件';
