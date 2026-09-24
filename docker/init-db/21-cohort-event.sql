-- =============================================================================
-- MAIDC - Cohort Event
-- File: 21-cohort-event.sql
-- Description: 专病队列动态事件表（PRD FR6 cohortDigest 数据源）。
--              记录队列视角的可展示事件：同步完成 / 知识库条目发布 / AI 入组建议待审。
--              埋点位置：DiseaseCohortService.matchPatients（SYNC_DONE）、
--              DiseaseKnowledgeService.publishItem（KB_ITEM_PUBLISHED，经 space.cohortId 关联队列，可为空）。
--              AI_SUGGEST_PENDING 为预留类型（AI 建议当前无持久化，产生待审实体后接入）。
--              无外键，业务层保证完整性（对齐 20-disease-followup.sql 惯例）。
-- =============================================================================

CREATE TABLE IF NOT EXISTS cdr.c_cohort_event (
    id              BIGSERIAL      PRIMARY KEY,
    cohort_id       BIGINT,                        -- 关联队列；KB 全局发布等无队列关联时为空
    event_type      VARCHAR(32)    NOT NULL CHECK (event_type IN ('SYNC_DONE','KB_ITEM_PUBLISHED','AI_SUGGEST_PENDING')),
    event_title     VARCHAR(256)   NOT NULL,
    event_detail    JSONB,
    created_by      VARCHAR(64)    NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN        NOT NULL DEFAULT FALSE,
    org_id          BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_cohort_event_created ON cdr.c_cohort_event(org_id, is_deleted, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_cohort_event_cohort ON cdr.c_cohort_event(cohort_id);

COMMENT ON TABLE cdr.c_cohort_event IS '专病队列动态事件表（工作台 cohortDigest 数据源）';
