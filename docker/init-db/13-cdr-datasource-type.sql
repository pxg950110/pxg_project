-- DDL for Data Source Management
-- This file creates tables for data source types, health checks, and adds columns to data source table

-- 1. Create table for data source types
CREATE TABLE IF NOT EXISTS cdr.r_data_source_type (
    id              BIGSERIAL       PRIMARY KEY,
    type_code       VARCHAR(64)     NOT NULL UNIQUE,
    type_name       VARCHAR(128)    NOT NULL,
    category        VARCHAR(32)     NOT NULL CHECK (category IN ('DATABASE','API','FILE')),
    icon            VARCHAR(64),
    param_schema    JSONB           NOT NULL,
    test_command    VARCHAR(32)     CHECK (test_command IN ('JDBC','HTTP','FILE_CHECK')),
    is_builtin      BOOLEAN         NOT NULL DEFAULT FALSE,
    sort_order      INT             NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)     NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL DEFAULT 0
);

COMMENT ON TABLE cdr.r_data_source_type IS '数据源类型配置表';
COMMENT ON COLUMN cdr.r_data_source_type.id IS '主键ID';
COMMENT ON COLUMN cdr.r_data_source_type.type_code IS '类型代码';
COMMENT ON COLUMN cdr.r_data_source_type.type_name IS '类型名称';
COMMENT ON COLUMN cdr.r_data_source_type.category IS '类型分类：DATABASE/FILE/API';
COMMENT ON COLUMN cdr.r_data_source_type.icon IS '图标名称';
COMMENT ON COLUMN cdr.r_data_source_type.param_schema IS '参数配置模板JSON';
COMMENT ON COLUMN cdr.r_data_source_type.test_command IS '测试命令类型';
COMMENT ON COLUMN cdr.r_data_source_type.is_builtin IS '是否内置类型';
COMMENT ON COLUMN cdr.r_data_source_type.sort_order IS '排序顺序';
COMMENT ON COLUMN cdr.r_data_source_type.created_by IS '创建人';
COMMENT ON COLUMN cdr.r_data_source_type.created_at IS '创建时间';
COMMENT ON COLUMN cdr.r_data_source_type.updated_by IS '更新人';
COMMENT ON COLUMN cdr.r_data_source_type.updated_at IS '更新时间';
COMMENT ON COLUMN cdr.r_data_source_type.is_deleted IS '是否删除';
COMMENT ON COLUMN cdr.r_data_source_type.org_id IS '组织ID';

-- 2. Add columns to existing data source table
ALTER TABLE cdr.r_data_source ADD COLUMN IF NOT EXISTS source_type_code VARCHAR(64);
ALTER TABLE cdr.r_data_source ADD COLUMN IF NOT EXISTS connection_params JSONB;

-- 3. Create table for data source health checks
CREATE TABLE IF NOT EXISTS cdr.r_data_source_health (
    id              BIGSERIAL       PRIMARY KEY,
    source_id       BIGINT          NOT NULL,
    check_type      VARCHAR(32)     NOT NULL CHECK (check_type IN ('MANUAL','SCHEDULED')),
    status          VARCHAR(16)     NOT NULL CHECK (status IN ('SUCCESS','FAIL','TIMEOUT')),
    latency_ms      INT,
    error_message   TEXT,
    checked_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    org_id          BIGINT          NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_ds_health_source ON cdr.r_data_source_health(source_id, checked_at DESC) WHERE NOT is_deleted;

COMMENT ON TABLE cdr.r_data_source_health IS '数据源健康检查记录表';
COMMENT ON COLUMN cdr.r_data_source_health.id IS '主键ID';
COMMENT ON COLUMN cdr.r_data_source_health.source_id IS '数据源ID';
COMMENT ON COLUMN cdr.r_data_source_health.check_type IS '检查类型：手动/定时';
COMMENT ON COLUMN cdr.r_data_source_health.status IS '检查状态';
COMMENT ON COLUMN cdr.r_data_source_health.latency_ms IS '延迟毫秒数';
COMMENT ON COLUMN cdr.r_data_source_health.error_message IS '错误信息';
COMMENT ON COLUMN cdr.r_data_source_health.checked_at IS '检查时间';
COMMENT ON COLUMN cdr.r_data_source_health.org_id IS '组织ID';
COMMENT ON COLUMN cdr.r_data_source_health.is_deleted IS '是否删除';

-- 4. Insert builtin data source types as seed data
-- MYSQL
INSERT INTO cdr.r_data_source_type (
    type_code, type_name, category, icon, param_schema, test_command, is_builtin, sort_order
) VALUES (
    'MYSQL', 'MySQL数据库', 'DATABASE', 'database',
    '{"fields":[{"key":"host","label":"主机地址","type":"text","required":true,"placeholder":"192.168.1.100"},{"key":"port","label":"端口","type":"number","required":true,"default":3306,"min":1,"max":65535},{"key":"database","label":"数据库名","type":"text","required":true},{"key":"username","label":"用户名","type":"text","required":true},{"key":"password","label":"密码","type":"password","required":true}]}',
    'JDBC', TRUE, 1
) ON CONFLICT (type_code) DO NOTHING;

-- POSTGRESQL
INSERT INTO cdr.r_data_source_type (
    type_code, type_name, category, icon, param_schema, test_command, is_builtin, sort_order
) VALUES (
    'POSTGRESQL', 'PostgreSQL数据库', 'DATABASE', 'database',
    '{"fields":[{"key":"host","label":"主机地址","type":"text","required":true,"placeholder":"192.168.1.100"},{"key":"port","label":"端口","type":"number","required":true,"default":5432,"min":1,"max":65535},{"key":"database","label":"数据库名","type":"text","required":true},{"key":"username","label":"用户名","type":"text","required":true},{"key":"password","label":"密码","type":"password","required":true}]}',
    'JDBC', TRUE, 2
) ON CONFLICT (type_code) DO NOTHING;

-- ORACLE
INSERT INTO cdr.r_data_source_type (
    type_code, type_name, category, icon, param_schema, test_command, is_builtin, sort_order
) VALUES (
    'ORACLE', 'Oracle数据库', 'DATABASE', 'database',
    '{"fields":[{"key":"host","label":"主机地址","type":"text","required":true},{"key":"port","label":"端口","type":"number","required":true,"default":1521,"min":1,"max":65535},{"key":"database","label":"SID/服务名","type":"text","required":true},{"key":"username","label":"用户名","type":"text","required":true},{"key":"password","label":"密码","type":"password","required":true}]}',
    'JDBC', TRUE, 3
) ON CONFLICT (type_code) DO NOTHING;

-- HTTP_API
INSERT INTO cdr.r_data_source_type (
    type_code, type_name, category, icon, param_schema, test_command, is_builtin, sort_order
) VALUES (
    'HTTP_API', 'HTTP接口', 'API', 'api',
    '{"fields":[{"key":"url","label":"接口地址","type":"text","required":true,"placeholder":"https://api.example.com/data"},{"key":"method","label":"请求方法","type":"select","required":true,"options":["GET","POST"],"default":"GET"},{"key":"headers","label":"请求头","type":"keyvalue","required":false},{"key":"body","label":"请求体","type":"textarea","required":false},{"key":"auth_type","label":"认证方式","type":"select","required":false,"options":["NONE","BASIC","BEARER"],"default":"NONE"},{"key":"auth_token","label":"Token","type":"password","required":false}]}',
    'HTTP', TRUE, 4
) ON CONFLICT (type_code) DO NOTHING;

-- CSV
INSERT INTO cdr.r_data_source_type (
    type_code, type_name, category, icon, param_schema, is_builtin, sort_order
) VALUES (
    'CSV', 'CSV文件', 'FILE', 'file-text',
    '{"fields":[{"key":"file_path","label":"文件路径","type":"text","required":true,"placeholder":"/data/import/patients.csv"},{"key":"encoding","label":"编码","type":"select","required":false,"options":["UTF-8","GBK","GB2312"],"default":"UTF-8"},{"key":"delimiter","label":"分隔符","type":"select","required":false,"options":[",","\\t","|"],"default":","}]}',
    TRUE, 5
) ON CONFLICT (type_code) DO NOTHING;

-- EXCEL
INSERT INTO cdr.r_data_source_type (
    type_code, type_name, category, icon, param_schema, is_builtin, sort_order
) VALUES (
    'EXCEL', 'Excel文件', 'FILE', 'file-excel',
    '{"fields":[{"key":"file_path","label":"文件路径","type":"text","required":true,"placeholder":"/data/import/patients.xlsx"},{"key":"sheet_name","label":"Sheet名称","type":"text","required":false,"placeholder":"默认第一个Sheet"}]}',
    TRUE, 6
) ON CONFLICT (type_code) DO NOTHING;

-- JSON_FILE
INSERT INTO cdr.r_data_source_type (
    type_code, type_name, category, icon, param_schema, is_builtin, sort_order
) VALUES (
    'JSON_FILE', 'JSON文件', 'FILE', 'file-code',
    '{"fields":[{"key":"file_path","label":"文件路径","type":"text","required":true,"placeholder":"/data/import/data.json"},{"key":"encoding","label":"编码","type":"select","required":false,"options":["UTF-8","GBK"],"default":"UTF-8"}]}',
    TRUE, 7
) ON CONFLICT (type_code) DO NOTHING;