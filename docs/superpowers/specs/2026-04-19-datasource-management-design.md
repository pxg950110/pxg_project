# 数据源管理增强设计

日期：2026-04-19
状态：待实现

## 概述

增强数据源管理模块，支持三种数据源类型（关系型数据库、HTTP API、文件），实现动态参数模板、真实连接测试、完整健康监控。

## 1. 数据库设计

### 1.1 新增 `cdr.r_data_source_type`（数据源类型模板）

```sql
CREATE TABLE cdr.r_data_source_type (
    id              BIGSERIAL PRIMARY KEY,
    type_code       VARCHAR(64)  NOT NULL UNIQUE,
    type_name       VARCHAR(128) NOT NULL,
    category        VARCHAR(32)  NOT NULL,          -- DATABASE / API / FILE
    icon            VARCHAR(64),
    param_schema    JSONB        NOT NULL,          -- 参数 JSON Schema
    test_command    VARCHAR(32),                     -- JDBC / HTTP / FILE_CHECK
    is_builtin      BOOLEAN      DEFAULT false,
    sort_order      INT          DEFAULT 0,
    org_id          BIGINT,
    created_by      VARCHAR(64),
    created_at      TIMESTAMP    DEFAULT now(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP    DEFAULT now(),
    is_deleted      BOOLEAN      DEFAULT false
);
```

### 1.2 `param_schema` 格式

简化的 field descriptor 数组，前端遍历渲染：

```json
{
  "fields": [
    {
      "key": "host",
      "label": "主机地址",
      "type": "text",
      "required": true,
      "placeholder": "192.168.1.100"
    }
  ]
}
```

支持的 field type：`text`、`password`、`number`、`select`、`textarea`、`keyvalue`。
额外属性：`required`、`placeholder`、`default`、`min`、`max`、`options`。

### 1.3 修改 `cdr.r_data_source`

新增两列，不删除旧列（兼容过渡）：

```sql
ALTER TABLE cdr.r_data_source ADD COLUMN source_type_code VARCHAR(64);
ALTER TABLE cdr.r_data_source ADD COLUMN connection_params JSONB;
```

- `source_type_code`：引用 `r_data_source_type.type_code`
- `connection_params`：存储实际连接参数，结构由对应类型的 `param_schema` 约束

### 1.4 新增 `cdr.r_data_source_health`（健康检查记录）

```sql
CREATE TABLE cdr.r_data_source_health (
    id              BIGSERIAL PRIMARY KEY,
    source_id       BIGINT       NOT NULL,
    check_type      VARCHAR(32)  NOT NULL,          -- MANUAL / SCHEDULED
    status          VARCHAR(16)  NOT NULL,          -- SUCCESS / FAIL / TIMEOUT
    latency_ms      INT,
    error_message   TEXT,
    checked_at      TIMESTAMP    NOT NULL DEFAULT now(),
    org_id          BIGINT,
    is_deleted      BOOLEAN      DEFAULT false
);
```

### 1.5 内置数据源类型

系统初始化时插入 7 种内置类型：

| type_code | type_name | category | test_command |
|-----------|-----------|----------|-------------|
| MYSQL | MySQL | DATABASE | JDBC |
| POSTGRESQL | PostgreSQL | DATABASE | JDBC |
| ORACLE | Oracle | DATABASE | JDBC |
| HTTP_API | HTTP API | API | HTTP |
| CSV | CSV 文件 | FILE | FILE_CHECK |
| EXCEL | Excel 文件 | FILE | FILE_CHECK |
| JSON_FILE | JSON 文件 | FILE | FILE_CHECK |

## 2. 后端架构

### 2.1 新增类

| 类 | 职责 |
|---|------|
| `DataSourceTypeEntity` | 类型模板实体 |
| `DataSourceHealthEntity` | 健康检查记录实体 |
| `DataSourceTypeRepository` | 类型模板 JPA Repository |
| `DataSourceHealthRepository` | 健康记录 JPA Repository |
| `DataSourceTypeService` | 类型模板 CRUD |
| `DataSourceHealthService` | 健康记录写入 + 统计查询 |
| `ConnectionTester` | 策略接口：`test(params) → ConnectionTestResult` |
| `JdbcConnectionTester` | JDBC 连接测试实现 |
| `HttpConnectionTester` | HTTP API 连接测试实现 |
| `FileConnectionTester` | 文件路径检查实现 |
| `ConnectionTesterFactory` | 按 test_command 分发到对应策略 |
| `DataSourceTypeController` | 类型模板管理 REST API |

### 2.2 修改类

| 类 | 变更 |
|---|------|
| `DataSourceEntity` | 新增 `sourceTypeCode`、`connectionParams` 字段 |
| `DataSourceService` | 创建时校验 connection_params 符合 param_schema 的 required 字段 |
| `DataSourceController` | 增强 test-connection 为真实测试，新增健康检查端点 |

### 2.3 策略模式

```java
public interface ConnectionTester {
    String getType();  // JDBC / HTTP / FILE_CHECK
    ConnectionTestResult test(Map<String, Object> params);
}

public record ConnectionTestResult(
    boolean success,
    String message,
    int latencyMs,
    Map<String, Object> details
) {}
```

Spring 自动收集所有 `ConnectionTester` 实现，`ConnectionTesterFactory` 按 `test_command` 字段分发。

### 2.4 API 端点

```
数据源类型管理：
  GET    /api/v1/cdr/datasource-types
  GET    /api/v1/cdr/datasource-types/{code}
  POST   /api/v1/cdr/datasource-types
  PUT    /api/v1/cdr/datasource-types/{code}
  DELETE /api/v1/cdr/datasource-types/{code}

数据源管理（增强）：
  POST   /api/v1/cdr/datasources/test-connection        创建前测试（不依赖 ID）
  POST   /api/v1/cdr/datasources/{id}/test-connection   已保存数据源测试
  GET    /api/v1/cdr/datasources/{id}/health             健康检查历史
  GET    /api/v1/cdr/datasources/{id}/health/stats       统计（可用率+延迟）
```

## 3. 前端架构

### 3.1 新增组件

| 组件 | 职责 |
|------|------|
| `DataSourceForm.vue` | 独立表单组件，先选类型再填参数 |
| `DynamicFormRenderer.vue` | 根据 param_schema 动态渲染表单项 |
| `HealthMonitor.vue` | 健康检查图表（ECharts 延迟折线图 + 可用率 + 记录表） |

### 3.2 DynamicFormRenderer 字段类型映射

| schema type | 渲染组件 |
|-------------|---------|
| text | a-input |
| password | a-input-password |
| number | a-input-number |
| select | a-select |
| textarea | a-textarea |
| keyvalue | 自定义键值对编辑器 |

### 3.3 创建/编辑流程

1. 用户点击"新建数据源"
2. 第一步：选择数据源类型（从 API 获取类型列表）
3. 选择后加载该类型的 `param_schema`
4. `DynamicFormRenderer` 根据 param_schema 渲染连接参数表单
5. 填写名称 + 描述 + 连接参数
6. 点击"测试连接" → `POST /datasources/test-connection`（含 type_code + params）
7. 连接成功后提交保存

### 3.4 健康监控 Tab（DataSourceDetail.vue）

- 顶部状态卡片：连接状态、当前延迟、30天可用率、上次检查时间
- ECharts 折线图：延迟趋势
- 表格：最近检查记录（时间、状态、延迟、错误信息）

### 3.5 新增前端 API

```typescript
getDataSourceTypes()
createDataSourceType(data)
updateDataSourceType(code, data)
deleteDataSourceType(code)
testConnection(data: { type_code, connection_params })
getDataSourceHealth(id, { limit })
getDataSourceHealthStats(id, { since })
```

## 4. 不做的事

- OAuth 复杂认证（仅 Basic/Bearer/None）
- 自动数据发现（扫描表结构）
- API 分页拉取策略
- 连接池管理
- 数据源分组/标签
