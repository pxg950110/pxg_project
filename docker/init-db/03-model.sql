-- ==================== model schema: 11 tables ====================

-- 1. m_model — 模型注册主表
CREATE TABLE model.m_model (
    id                BIGSERIAL      PRIMARY KEY,
    model_code        VARCHAR(32)    NOT NULL,
    model_name        VARCHAR(128)   NOT NULL,
    description       TEXT,
    model_type        VARCHAR(32)    NOT NULL,
    task_type         VARCHAR(32),
    framework         VARCHAR(32),
    input_schema      JSONB,
    output_schema     JSONB,
    tags              VARCHAR(256),
    license           VARCHAR(32),
    owner_id          BIGINT         NOT NULL DEFAULT 0,
    project_id        BIGINT         REFERENCES rdr.r_study_project(id),
    status            VARCHAR(16)    NOT NULL DEFAULT 'DRAFT',
    created_by        VARCHAR(64)    NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN        NOT NULL DEFAULT FALSE,
    org_id            BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT uk_model_code UNIQUE (org_id, model_code),
    CONSTRAINT chk_model_type CHECK (model_type IN ('IMAGING','NLP','GENOMIC','STRUCTURED','MULTIMODAL')),
    CONSTRAINT chk_framework  CHECK (framework  IN ('PYTORCH','TENSORFLOW','SKLEARN','XGBOOST','ONNX','OTHER')),
    CONSTRAINT chk_model_status CHECK (status IN ('DRAFT','REGISTERED','PUBLISHED','DEPRECATED'))
);
COMMENT ON TABLE model.m_model IS '模型注册主表';

-- 2. m_model_version — 模型版本表
CREATE TABLE model.m_model_version (
    id                  BIGSERIAL      PRIMARY KEY,
    model_id            BIGINT         NOT NULL REFERENCES model.m_model(id),
    version_no          VARCHAR(16)    NOT NULL,
    description         TEXT,
    changelog           TEXT,
    framework_version   VARCHAR(32),
    model_file_path     VARCHAR(256),
    model_file_size     BIGINT,
    model_file_checksum VARCHAR(64),
    config_path         VARCHAR(256),
    hyper_params        JSONB,
    training_dataset_id BIGINT         REFERENCES rdr.r_dataset(id),
    training_metrics    JSONB,
    status              VARCHAR(16)    NOT NULL DEFAULT 'CREATED',
    created_by          VARCHAR(64)    NOT NULL DEFAULT 'system',
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    org_id              BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT uk_model_version UNIQUE (model_id, version_no),
    CONSTRAINT chk_version_status CHECK (status IN ('CREATED','TRAINING','EVALUATING','APPROVED','DEPLOYED','DEPRECATED'))
);
CREATE INDEX idx_mv_model_id ON model.m_model_version(model_id);
COMMENT ON TABLE model.m_model_version IS '模型版本表';

-- 3. m_evaluation — 模型评估表
CREATE TABLE model.m_evaluation (
    id               BIGSERIAL      PRIMARY KEY,
    model_id         BIGINT         NOT NULL REFERENCES model.m_model(id),
    version_id       BIGINT         NOT NULL REFERENCES model.m_model_version(id),
    eval_name        VARCHAR(128)   NOT NULL,
    eval_type        VARCHAR(32)    NOT NULL,
    dataset_id       BIGINT,
    metrics          JSONB,
    confusion_matrix JSONB,
    roc_data         JSONB,
    report_url       VARCHAR(256),
    status           VARCHAR(16)    NOT NULL DEFAULT 'PENDING',
    started_at       TIMESTAMP,
    completed_at     TIMESTAMP,
    created_by       VARCHAR(64)    NOT NULL DEFAULT 'system',
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_by       VARCHAR(64),
    updated_at       TIMESTAMP,
    is_deleted       BOOLEAN        NOT NULL DEFAULT FALSE,
    org_id           BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT chk_eval_type CHECK (eval_type IN ('HOLDOUT','CROSS_VALIDATION','EXTERNAL')),
    CONSTRAINT chk_eval_status CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED'))
);
CREATE INDEX idx_eval_version_id ON model.m_evaluation(version_id);
COMMENT ON TABLE model.m_evaluation IS '模型评估表';

-- 4. m_approval — 模型审批表
CREATE TABLE model.m_approval (
    id               BIGSERIAL      PRIMARY KEY,
    model_id         BIGINT         NOT NULL REFERENCES model.m_model(id),
    version_id       BIGINT         NOT NULL REFERENCES model.m_model_version(id),
    eval_id          BIGINT         REFERENCES model.m_evaluation(id),
    approval_type    VARCHAR(32)    NOT NULL,
    current_level    INT            NOT NULL DEFAULT 1,
    evidence_docs    JSONB,
    risk_assessment  JSONB,
    status           VARCHAR(16)    NOT NULL DEFAULT 'PENDING',
    submitted_by     BIGINT         NOT NULL DEFAULT 0,
    submitted_at     TIMESTAMP,
    reviewed_by      BIGINT,
    reviewed_at      TIMESTAMP,
    review_comment   TEXT,
    created_by       VARCHAR(64)    NOT NULL DEFAULT 'system',
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_by       VARCHAR(64),
    updated_at       TIMESTAMP,
    is_deleted       BOOLEAN        NOT NULL DEFAULT FALSE,
    org_id           BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT chk_approval_type CHECK (approval_type IN ('DEPLOY','PUBLISH')),
    CONSTRAINT chk_approval_status CHECK (status IN ('PENDING','APPROVED','REJECTED'))
);
CREATE INDEX idx_approval_version_id ON model.m_approval(version_id);
COMMENT ON TABLE model.m_approval IS '模型审批表';

-- 5. m_deployment — 模型部署表
CREATE TABLE model.m_deployment (
    id                 BIGSERIAL      PRIMARY KEY,
    model_id           BIGINT         NOT NULL REFERENCES model.m_model(id),
    version_id         BIGINT         NOT NULL REFERENCES model.m_model_version(id),
    deployment_name    VARCHAR(128)   NOT NULL,
    environment        VARCHAR(32)    NOT NULL,
    resource_config    JSONB,
    endpoint_url       VARCHAR(256),
    replicas           INT            NOT NULL DEFAULT 1,
    status             VARCHAR(16)    NOT NULL DEFAULT 'CREATING',
    started_at         TIMESTAMP,
    stopped_at         TIMESTAMP,
    last_health_check  TIMESTAMP,
    created_by         VARCHAR(64)    NOT NULL DEFAULT 'system',
    created_at         TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_by         VARCHAR(64),
    updated_at         TIMESTAMP,
    is_deleted         BOOLEAN        NOT NULL DEFAULT FALSE,
    org_id             BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT chk_deploy_env CHECK (environment IN ('DEV','STAGING','PRODUCTION')),
    CONSTRAINT chk_deploy_status CHECK (status IN ('CREATING','RUNNING','STOPPED','SCALING','FAILED'))
);
CREATE INDEX idx_deploy_model_id ON model.m_deployment(model_id);
COMMENT ON TABLE model.m_deployment IS '模型部署表';

-- 6. m_deploy_route — 部署路由表 (灰度 / A-B / 加权)
CREATE TABLE model.m_deploy_route (
    id                    BIGSERIAL      PRIMARY KEY,
    route_name            VARCHAR(128)   NOT NULL,
    route_type            VARCHAR(32)    NOT NULL,
    config                JSONB,
    active_deployment_id  BIGINT         REFERENCES model.m_deployment(id),
    canary_deployment_id  BIGINT         REFERENCES model.m_deployment(id),
    canary_weight         INT            NOT NULL DEFAULT 0,
    status                VARCHAR(16)    NOT NULL DEFAULT 'ACTIVE',
    created_by            VARCHAR(64)    NOT NULL DEFAULT 'system',
    created_at            TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_by            VARCHAR(64),
    updated_at            TIMESTAMP,
    is_deleted            BOOLEAN        NOT NULL DEFAULT FALSE,
    org_id                BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT chk_route_type CHECK (route_type IN ('CANARY','A_B_TEST','WEIGHTED'))
);
COMMENT ON TABLE model.m_deploy_route IS '部署路由表（灰度/A-B测试/加权）';

-- 7. m_inference_log — 推理日志分区表
CREATE TABLE model.m_inference_log (
    id               BIGSERIAL,
    deployment_id    BIGINT         NOT NULL,
    request_id       VARCHAR(64)    NOT NULL,
    patient_id       BIGINT,
    encounter_id     BIGINT,
    input_summary    JSONB,
    output_result    JSONB          NOT NULL,
    confidence       DECIMAL(5,4),
    latency_ms       INT,
    model_version_no VARCHAR(16),
    caller_service   VARCHAR(64),
    caller_user_id   BIGINT,
    status           VARCHAR(16)    NOT NULL DEFAULT 'SUCCESS',
    error_message    TEXT,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    org_id           BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);
COMMENT ON TABLE model.m_inference_log IS '推理日志分区表（按月分区）';

-- Monthly partitions: 2026-01 ~ 2026-04
CREATE TABLE model.m_inference_log_2026_01 PARTITION OF model.m_inference_log
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE model.m_inference_log_2026_02 PARTITION OF model.m_inference_log
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE model.m_inference_log_2026_03 PARTITION OF model.m_inference_log
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE model.m_inference_log_2026_04 PARTITION OF model.m_inference_log
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
-- Default partition
CREATE TABLE model.m_inference_log_default PARTITION OF model.m_inference_log DEFAULT;

-- Partition indexes
CREATE INDEX idx_il_deploy_created ON model.m_inference_log(deployment_id, created_at);
CREATE INDEX idx_il_request_id    ON model.m_inference_log(request_id);

-- 8. m_model_metric — 模型运行指标
CREATE TABLE model.m_model_metric (
    id             BIGSERIAL      PRIMARY KEY,
    model_id       BIGINT         NOT NULL,
    version_id     BIGINT,
    deployment_id  BIGINT,
    metric_type    VARCHAR(32)    NOT NULL,
    metric_name    VARCHAR(64)    NOT NULL,
    metric_value   DECIMAL(12,4)  NOT NULL,
    collected_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
    org_id         BIGINT         NOT NULL DEFAULT 0
);
CREATE INDEX idx_mm_deploy_collected ON model.m_model_metric(deployment_id, collected_at);
COMMENT ON TABLE model.m_model_metric IS '模型运行指标表';

-- 9. m_alert_rule — 告警规则表
CREATE TABLE model.m_alert_rule (
    id                BIGSERIAL      PRIMARY KEY,
    rule_name         VARCHAR(128)   NOT NULL,
    rule_type         VARCHAR(32)    NOT NULL,
    target_type       VARCHAR(32)    NOT NULL,
    target_id         BIGINT         NOT NULL,
    condition_expr    JSONB          NOT NULL,
    severity          VARCHAR(16)    NOT NULL,
    notify_channels   JSONB,
    enabled           BOOLEAN        NOT NULL DEFAULT TRUE,
    last_triggered_at TIMESTAMP,
    created_by        VARCHAR(64)    NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN        NOT NULL DEFAULT FALSE,
    org_id            BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT chk_rule_type CHECK (rule_type IN ('LATENCY','ERROR_RATE','QPS_DRIFT','CONFIDENCE_DRIFT')),
    CONSTRAINT chk_target_type CHECK (target_type IN ('DEPLOYMENT','MODEL')),
    CONSTRAINT chk_severity CHECK (severity IN ('INFO','WARNING','CRITICAL'))
);
COMMENT ON TABLE model.m_alert_rule IS '告警规则表';

-- 10. m_alert_record — 告警记录表
CREATE TABLE model.m_alert_record (
    id               BIGSERIAL      PRIMARY KEY,
    rule_id          BIGINT         NOT NULL REFERENCES model.m_alert_rule(id),
    alert_title      VARCHAR(256)   NOT NULL,
    alert_detail     TEXT,
    severity         VARCHAR(16)    NOT NULL,
    status           VARCHAR(16)    NOT NULL DEFAULT 'PENDING',
    triggered_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    acknowledged_by  BIGINT,
    acknowledged_at  TIMESTAMP,
    resolved_at      TIMESTAMP,
    org_id           BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT chk_alert_severity CHECK (severity IN ('INFO','WARNING','CRITICAL')),
    CONSTRAINT chk_alert_status CHECK (status IN ('PENDING','ACKNOWLEDGED','RESOLVED'))
);
CREATE INDEX idx_ar_rule_id ON model.m_alert_record(rule_id);
COMMENT ON TABLE model.m_alert_record IS '告警记录表';

-- 11. m_model_tag — 模型标签多对多关联表
CREATE TABLE model.m_model_tag (
    id       BIGSERIAL    PRIMARY KEY,
    model_id BIGINT       NOT NULL REFERENCES model.m_model(id),
    tag      VARCHAR(64)  NOT NULL,
    org_id   BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_mt_tag ON model.m_model_tag(tag);
CREATE INDEX idx_mt_model_id ON model.m_model_tag(model_id);
COMMENT ON TABLE model.m_model_tag IS '模型标签多对多关联表';
