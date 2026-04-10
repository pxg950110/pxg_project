-- =====================================================
-- MAIDC 性能优化 — 分区表维护 (pg_partman)
-- 推理日志表按月分区，自动创建未来分区
-- =====================================================

-- 1. 安装 pg_partman 扩展
CREATE EXTENSION IF NOT EXISTS pg_partman;

-- 2. 推理日志表按月分区 (m_inference_log 表需先转为分区父表)
-- 注意：已有表需要迁移，建议在低峰期执行

-- 创建分区配置（按月分区，预创建 3 个月）
SELECT partman.create_parent(
    p_parent_table := 'model.m_inference_log',
    p_control := 'created_at',
    p_type := 'range',
    p_interval := '1 month',
    p_premake := 3,
    p_start_partition := to_char(date_trunc('month', CURRENT_DATE), 'YYYY-MM-DD')
);

-- 3. 配置自动维护（pg_partman bgw 会在后台自动创建新分区）
-- 在 postgresql.conf 中配置:
-- shared_preload_libraries = 'pg_partman_bgw'
-- pg_partman.bgw_role = 'maidc_admin'
-- pg_partman.bgw_interval = 3600  -- 每小时检查一次

-- 4. 手动运行一次分区维护（可选）
CALL partman.run_maintenance_proc();

-- 5. 审计日志表按月分区
SELECT partman.create_parent(
    p_parent_table := 'audit.a_audit_log',
    p_control := 'created_at',
    p_type := 'range',
    p_interval := '1 month',
    p_premake := 3,
    p_start_partition := to_char(date_trunc('month', CURRENT_DATE), 'YYYY-MM-DD')
);

-- 6. 数据访问日志按月分区
SELECT partman.create_parent(
    p_parent_table := 'audit.a_data_access_log',
    p_control := 'access_time',
    p_type := 'range',
    p_interval := '1 month',
    p_premake := 3,
    p_start_partition := to_char(date_trunc('month', CURRENT_DATE), 'YYYY-MM-DD')
);

-- =====================================================
-- 慢查询优化 — 关键索引补充
-- =====================================================

-- 模型表：按状态+类型复合查询
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_model_status_type
    ON model.m_model (status, model_type) WHERE is_deleted = false;

-- 推理日志：按部署ID+时间范围查询
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inference_deploy_time
    ON model.m_inference_log (deployment_id, created_at DESC);

-- 患者表：姓名模糊搜索（pg_trgm 扩展）
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_patient_name_trgm
    ON cdr.c_patient USING gin (patient_name gin_trgm_ops);

-- 就诊记录：按患者+日期查询
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_encounter_patient_date
    ON cdr.c_encounter (patient_id, encounter_date DESC) WHERE is_deleted = false;

-- 审计日志：按操作人+时间查询
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_operator_time
    ON audit.a_audit_log (operator_id, created_at DESC);

-- 审计日志：按模块+操作类型查询
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_module_action
    ON audit.a_audit_log (module, action);

-- 研究项目：按状态+负责人查询
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_project_status_pi
    ON rdr.r_study_project (status, principal_investigator) WHERE is_deleted = false;

-- 数据集：按项目+状态查询
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_dataset_project_status
    ON rdr.r_dataset (project_id, status) WHERE is_deleted = false;

-- =====================================================
-- 定期维护任务 (VACUUM + ANALYZE)
-- 建议通过 cron 或 pg_partman 钩子执行
-- =====================================================

-- 每日 VACUUM ANALYZE 高频写入表
-- crontab: 0 3 * * * psql -c "VACUUM ANALYZE model.m_inference_log;"
-- crontab: 0 3 * * * psql -c "VACUUM ANALYZE audit.a_audit_log;"

-- 分区表旧数据归档（保留 12 个月推理日志）
-- SELECT partman.drop_partition_time('model.m_inference_log', '12 months'::interval, true);
