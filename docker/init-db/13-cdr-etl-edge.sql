-- =============================================================================
-- MAIDC - CDR ETL Edge (Visual Designer Connections)
-- File: 13-cdr-etl-edge.sql
-- Description: 可视化ETL设计器的连线表，存储节点之间的连接关系
-- =============================================================================

CREATE TABLE IF NOT EXISTS cdr.r_etl_edge (
    id              BIGSERIAL       PRIMARY KEY,
    pipeline_id     BIGINT          NOT NULL,
    source_step_id  BIGINT          NOT NULL,
    source_port     VARCHAR(32)     NOT NULL DEFAULT 'out_1',
    target_step_id  BIGINT          NOT NULL,
    target_port     VARCHAR(32)     NOT NULL DEFAULT 'in_1',
    field_mappings  JSONB,
    sort_order      INT             NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)     NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

CREATE INDEX idx_etl_edge_pipeline ON cdr.r_etl_edge(pipeline_id) WHERE NOT is_deleted;
CREATE INDEX idx_etl_edge_source   ON cdr.r_etl_edge(source_step_id) WHERE NOT is_deleted;
CREATE INDEX idx_etl_edge_target   ON cdr.r_etl_edge(target_step_id) WHERE NOT is_deleted;

COMMENT ON TABLE cdr.r_etl_edge IS 'ETL连线 - 可视化设计器中节点之间的数据连接';

-- Add edge_id column to field mapping table
ALTER TABLE cdr.r_etl_field_mapping ADD COLUMN IF NOT EXISTS edge_id BIGINT;
