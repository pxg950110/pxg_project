# Plan 01: DDL + 种子数据

**Goal:** 创建 3 张新表（r_data_source_type, r_data_source_health）+ 修改 r_data_source + 插入 7 种内置类型

**Files:**
- Create: `docker/init-db/13-cdr-datasource-type.sql`

---

### Task 1: 创建 SQL 文件

- [ ] **Step 1: 写 DDL**

创建 `docker/init-db/13-cdr-datasource-type.sql`：

```sql
-- =============================================================================
-- MAIDC - 数据源类型管理 + 健康监控 DDL
-- =============================================================================

-- 1. 数据源类型模板
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

COMMENT ON TABLE cdr.r_data_source_type IS '数据源类型模板 - 定义每种数据源的参数结构';

-- 2. 数据源表新增列
ALTER TABLE cdr.r_data_source ADD COLUMN IF NOT EXISTS source_type_code VARCHAR(64);
ALTER TABLE cdr.r_data_source ADD COLUMN IF NOT EXISTS connection_params JSONB;

-- 3. 健康检查记录
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

CREATE INDEX idx_ds_health_source ON cdr.r_data_source_health(source_id, checked_at DESC)
    WHERE NOT is_deleted;

COMMENT ON TABLE cdr.r_data_source_health IS '数据源健康检查记录';

-- 4. 内置类型种子数据
INSERT INTO cdr.r_data_source_type (type_code, type_name, category, icon, test_command, is_builtin, sort_order, param_schema)
VALUES
('MYSQL', 'MySQL', 'DATABASE', 'DatabaseOutlined', 'JDBC', true, 1,
 '{"fields":[{"key":"host","label":"主机地址","type":"text","required":true,"placeholder":"192.168.1.100"},{"key":"port","label":"端口","type":"number","required":true,"default":3306,"min":1,"max":65535},{"key":"database","label":"数据库名","type":"text","required":true},{"key":"username","label":"用户名","type":"text","required":true},{"key":"password","label":"密码","type":"password","required":true}]}'),

('POSTGRESQL', 'PostgreSQL', 'DATABASE', 'DatabaseOutlined', 'JDBC', true, 2,
 '{"fields":[{"key":"host","label":"主机地址","type":"text","required":true,"placeholder":"192.168.1.100"},{"key":"port","label":"端口","type":"number","required":true,"default":5432,"min":1,"max":65535},{"key":"database","label":"数据库名","type":"text","required":true},{"key":"username","label":"用户名","type":"text","required":true},{"key":"password","label":"密码","type":"password","required":true}]}'),

('ORACLE', 'Oracle', 'DATABASE', 'DatabaseOutlined', 'JDBC', true, 3,
 '{"fields":[{"key":"host","label":"主机地址","type":"text","required":true},{"key":"port","label":"端口","type":"number","required":true,"default":1521,"min":1,"max":65535},{"key":"database","label":"SID/服务名","type":"text","required":true},{"key":"username","label":"用户名","type":"text","required":true},{"key":"password","label":"密码","type":"password","required":true}]}'),

('HTTP_API', 'HTTP API', 'API', 'ApiOutlined', 'HTTP', true, 4,
 '{"fields":[{"key":"url","label":"接口地址","type":"text","required":true,"placeholder":"https://api.example.com/data"},{"key":"method","label":"请求方法","type":"select","required":true,"options":["GET","POST"],"default":"GET"},{"key":"headers","label":"请求头","type":"keyvalue","required":false},{"key":"body","label":"请求体","type":"textarea","required":false},{"key":"auth_type","label":"认证方式","type":"select","required":false,"options":["NONE","BASIC","BEARER"],"default":"NONE"},{"key":"auth_token","label":"Token","type":"password","required":false}]}'),

('CSV', 'CSV 文件', 'FILE', 'FileTextOutlined', 'FILE_CHECK', true, 5,
 '{"fields":[{"key":"file_path","label":"文件路径","type":"text","required":true,"placeholder":"/data/import/patients.csv"},{"key":"encoding","label":"编码","type":"select","required":false,"options":["UTF-8","GBK","GB2312"],"default":"UTF-8"},{"key":"delimiter","label":"分隔符","type":"select","required":false,"options":[",","\\t","|"],","]"default":","}]}'),

('EXCEL', 'Excel 文件', 'FILE', 'FileExcelOutlined', 'FILE_CHECK', true, 6,
 '{"fields":[{"key":"file_path","label":"文件路径","type":"text","required":true,"placeholder":"/data/import/patients.xlsx"},{"key":"sheet_name","label":"Sheet名称","type":"text","required":false,"placeholder":"默认第一个Sheet"}]}'),

('JSON_FILE', 'JSON 文件', 'FILE', 'FileTextOutlined', 'FILE_CHECK', true, 7,
 '{"fields":[{"key":"file_path","label":"文件路径","type":"text","required":true,"placeholder":"/data/import/data.json"},{"key":"encoding","label":"编码","type":"select","required":false,"options":["UTF-8","GBK"],"default":"UTF-8"}]}')
ON CONFLICT (type_code) DO NOTHING;
```

注意：CSV/EXCEL 的 param_schema 中分隔符的 JSON 写法需要微调——实际文件中把 `","\\t","|"],","]"default":","}` 修正为合法 JSON。这里展示的是 CSV 的分隔符选项。实际编写时确保 JSON 合法。

- [ ] **Step 2: 修正 JSON 语法，确保 param_schema 合法**

重点检查每个 INSERT 的 param_schema 列是合法 JSON。

- [ ] **Step 3: 提交**

```bash
git add docker/init-db/13-cdr-datasource-type.sql
git commit -m "feat(datasource): add DDL for data source types, health checks, and seed data"
```
