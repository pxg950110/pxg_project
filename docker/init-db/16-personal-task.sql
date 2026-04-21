-- 16-personal-task.sql — 个人待办任务表
CREATE TABLE system.t_personal_task (
    id              BIGSERIAL      PRIMARY KEY,
    title           VARCHAR(200)   NOT NULL,
    description     TEXT,
    task_type       VARCHAR(20)    NOT NULL,
    priority        VARCHAR(10)    NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(10)    NOT NULL DEFAULT 'PENDING',
    assignee_id     BIGINT         NOT NULL,
    source_id       BIGINT,
    source_type     VARCHAR(20),
    due_date        TIMESTAMP,
    created_by      VARCHAR(64)    NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN        NOT NULL DEFAULT FALSE,
    org_id          BIGINT         NOT NULL DEFAULT 1,
    CONSTRAINT chk_pt_type     CHECK (task_type IN ('APPROVAL','LABELING','DATA_QUERY','OTHER')),
    CONSTRAINT chk_pt_priority CHECK (priority IN ('HIGH','MEDIUM','LOW')),
    CONSTRAINT chk_pt_status   CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','CANCELLED'))
);

CREATE INDEX idx_pt_assignee_status ON system.t_personal_task(assignee_id, status);
CREATE INDEX idx_pt_source ON system.t_personal_task(source_type, source_id);

COMMENT ON TABLE system.t_personal_task IS '个人待办任务表';
