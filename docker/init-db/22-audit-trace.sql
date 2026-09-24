-- 22-audit-trace.sql
-- 日志链路管理：审计三表 trace_id（数据访问日志、系统事件补齐链路字段）
-- 幂等：可重复执行；存量环境需手动执行一次（docker exec -i <postgres容器> psql -U maidc -d maidc < 22-audit-trace.sql）
-- a_audit_log.trace_id 已在 06-audit.sql 中定义（NOT NULL + 索引），此处无需变更

ALTER TABLE audit.a_data_access_log ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64);
ALTER TABLE audit.a_system_event    ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64);

-- 等值过滤走索引；partial index 避免历史 NULL 行拖累索引体积
CREATE INDEX IF NOT EXISTS idx_a_data_access_log_trace_id
    ON audit.a_data_access_log (trace_id) WHERE trace_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_a_system_event_trace_id
    ON audit.a_system_event (trace_id) WHERE trace_id IS NOT NULL;
