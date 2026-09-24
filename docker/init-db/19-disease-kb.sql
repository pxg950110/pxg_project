-- ============================================================
-- 19-disease-kb.sql
-- 专病知识库：知识空间 / 知识条目 / RAG 分块 / AI 问答会话
-- 设计依据: 过程文件/2026-09-08/2026-09-08_需求设计_02_功能需求.md 第八章
-- ============================================================

-- 1. 专病知识空间
CREATE TABLE IF NOT EXISTS cdr.c_disease_kb_space (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    icd_codes       TEXT[],
    cohort_id       BIGINT,                        -- 关联 cdr.c_disease_cohort.id（可空）
    icon_color      VARCHAR(16)  NOT NULL DEFAULT '#2d5afa',
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE / INACTIVE
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_disease_kb_space_name UNIQUE (name, org_id)
);
COMMENT ON TABLE cdr.c_disease_kb_space IS '专病知识空间（与专病队列 1:0..1 联动）';

CREATE INDEX idx_dkb_space_cohort ON cdr.c_disease_kb_space(cohort_id);
CREATE INDEX idx_dkb_space_icd    ON cdr.c_disease_kb_space USING gin(icd_codes);

-- 2. 知识条目
CREATE TABLE IF NOT EXISTS cdr.c_disease_kb_item (
    id              BIGSERIAL    PRIMARY KEY,
    space_id        BIGINT       NOT NULL,
    item_type       VARCHAR(32)  NOT NULL,          -- GUIDELINE / LITERATURE / PATHWAY / SCALE
    title           VARCHAR(512) NOT NULL,
    summary         TEXT,                           -- 人工摘要
    ai_summary      TEXT,                           -- AI 生成摘要
    ai_extract      JSONB,                          -- AI 知识抽取结构化结果
    ai_status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING',   -- PENDING / DONE / FAILED
    content         TEXT,
    source          VARCHAR(256),
    authors         VARCHAR(512),
    publish_date    DATE,
    version_no      VARCHAR(32),
    tags            JSONB,
    file_url        VARCHAR(512),                   -- MinIO 对象地址
    file_name       VARCHAR(256),
    status          VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',     -- DRAFT / PUBLISHED / ARCHIVED
    view_count      INT          NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE cdr.c_disease_kb_item IS '专病知识条目（指南/共识、文献、诊疗路径、量表）';

CREATE INDEX idx_dkb_item_space ON cdr.c_disease_kb_item(space_id, item_type, status);
CREATE INDEX idx_dkb_item_tags  ON cdr.c_disease_kb_item USING gin(tags);
CREATE INDEX idx_dkb_item_fts   ON cdr.c_disease_kb_item
  USING gin(to_tsvector('zh', coalesce(title,'') || ' ' || coalesce(summary,'') || ' ' || coalesce(content,'')));

-- 3. RAG 分块（由 ai-worker 直接读写，Java 侧不建实体）
CREATE TABLE IF NOT EXISTS cdr.c_disease_kb_item_chunk (
    id              BIGSERIAL    PRIMARY KEY,
    item_id         BIGINT       NOT NULL,
    chunk_index     INT          NOT NULL,
    chunk_text      TEXT         NOT NULL,
    tokens          INT,
    embedding       vector(1024),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_dkb_chunk UNIQUE (item_id, chunk_index)
);
COMMENT ON TABLE cdr.c_disease_kb_item_chunk IS '知识条目 RAG 分块（pgvector 向量）';

CREATE INDEX idx_dkb_chunk_item ON cdr.c_disease_kb_item_chunk(item_id);
CREATE INDEX idx_dkb_chunk_vec  ON cdr.c_disease_kb_item_chunk USING hnsw(embedding vector_cosine_ops);

-- 4. AI 问答会话
CREATE TABLE IF NOT EXISTS cdr.c_disease_kb_qa_session (
    id              BIGSERIAL    PRIMARY KEY,
    space_id        BIGINT       NOT NULL,
    title           VARCHAR(256),
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE cdr.c_disease_kb_qa_session IS '专病 AI 问答会话';

CREATE INDEX idx_dkb_qa_session_space ON cdr.c_disease_kb_qa_session(space_id);

-- 5. AI 问答消息
CREATE TABLE IF NOT EXISTS cdr.c_disease_kb_qa_message (
    id              BIGSERIAL    PRIMARY KEY,
    session_id      BIGINT       NOT NULL,
    role            VARCHAR(16)  NOT NULL,          -- USER / ASSISTANT
    content         TEXT         NOT NULL,
    citations       JSONB,                          -- [{itemId, title, snippet, chunkId}]
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE cdr.c_disease_kb_qa_message IS '专病 AI 问答消息';

CREATE INDEX idx_dkb_qa_msg_session ON cdr.c_disease_kb_qa_message(session_id);

-- ============================================================
-- 权限码种子（沿用 17-permission-system.sql 的 s_permission 结构）
-- ============================================================
INSERT INTO system.s_permission (permission_code, permission_name, resource_type, resource_key, action, sort_order, created_by, org_id) VALUES
('cdr:diseasekb:read','专病知识库查看','MENU','/data/cdr/disease-kb','READ',13,'system',0),
('cdr:diseasekb:manage','专病知识库管理','BUTTON','/data/cdr/disease-kb/manage','UPDATE',14,'system',0),
('cdr:diseasekb:ai','专病知识库AI问答','BUTTON','/data/cdr/disease-kb/ai','EXECUTE',15,'system',0)
ON CONFLICT DO NOTHING;

-- 角色-权限映射：管理员全量，医生 read+ai
INSERT INTO system.s_role_permission (role_id, permission_id, org_id)
SELECT r.id, p.id, 0
FROM system.s_role r
JOIN system.s_permission p ON p.permission_code IN ('cdr:diseasekb:read','cdr:diseasekb:manage','cdr:diseasekb:ai')
WHERE r.role_code = 'admin'
  AND NOT EXISTS (SELECT 1 FROM system.s_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO system.s_role_permission (role_id, permission_id, org_id)
SELECT r.id, p.id, 0
FROM system.s_role r
JOIN system.s_permission p ON p.permission_code IN ('cdr:diseasekb:read','cdr:diseasekb:ai')
WHERE r.role_code IN ('doctor', 'researcher')
  AND NOT EXISTS (SELECT 1 FROM system.s_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
