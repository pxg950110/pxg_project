-- =============================================================================
-- MAIDC - Audit Schema Patch
-- File: 18-audit-event-type.sql
-- Description: audit.a_system_event 的 event_type CHECK 约束扩展，
--              支持原生 PERMISSION_DENIED（越权拒绝）事件类型。
--              保留 06-audit.sql 原有全部枚举值并追加 PERMISSION_DENIED；
--              event_level 不在本 patch 范围（WARN 已合法）。
--              兼容两种环境：
--              - 全新环境：06-audit.sql 创建的默认名约束先被 DROP 再重建；
--              - 存量环境（约束不存在）：DROP IF EXISTS 静默跳过后直接 ADD。
-- =============================================================================

BEGIN;

ALTER TABLE audit.a_system_event
    DROP CONSTRAINT IF EXISTS a_system_event_event_type_check;

ALTER TABLE audit.a_system_event
    ADD CONSTRAINT a_system_event_event_type_check
    CHECK (event_type IN ('SERVICE_START','SERVICE_STOP','CONFIG_CHANGE','DEPLOY','ALERT','PERMISSION_DENIED'));

COMMIT;
