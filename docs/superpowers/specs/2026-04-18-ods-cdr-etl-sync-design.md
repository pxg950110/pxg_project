# ODS→CDR ETL 同步管理设计

> 日期: 2026-04-18
> 状态: 已确认
> 范围: 全栈设计（数据模型 + API + 后端服务 + 前端页面 + ETL 引擎集成）

## 1. 需求概述

### 1.1 背景

MAIDC 系统的 ODS（操作数据存储层）接收来自不同来源的原始医疗数据（MIMIC-III、MIMIC-IV、HIS、LIS、PACS 等），需要一套通用的 ETL 管道将这些数据清洗、转换后写入 CDR（临床数据仓库）。

### 1.2 核心需求

| 维度 | 决定 |
|------|------|
| 同步类型 | 跨库 ETL 管道（外部数据库 → ODS → CDR） |
| 映射复杂度 | 多对多（1:1 / 1:N / N:1） |
| 执行引擎 | 可插拔，默认 Embulk |
| 设计范围 | 全栈（数据模型 + API + 前端页面） |
| 配置方式 | 可视化配置为主 |
| 架构模型 | 管道-步骤模型（Pipeline → Step） |

## 2. 数据模型

### 2.1 架构选择：管道-步骤模型

将 ETL 拆解为 Pipeline → Step 两层：
- 一个 Pipeline 对应一个数据源到 CDR 的完整链路
- Pipeline 内含多个 Step，每个 Step 是一组表映射
- Step 内定义字段映射规则 + 转换函数
- 运行时生成 Embulk 配置（YAML），调用 Embulk 执行

### 2.2 核心表（`cdr` schema）

#### r_etl_pipeline（ETL 管道）

| 列名 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 主键 |
| pipeline_name | VARCHAR(128) NOT NULL | 管道名称 |
| source_id | BIGINT NOT NULL | 关联数据源（r_data_source.id） |
| description | TEXT | 描述 |
| engine_type | VARCHAR(16) NOT NULL DEFAULT 'EMBULK' | 引擎类型：EMBULK / SPARK / PYTHON |
| status | VARCHAR(16) NOT NULL DEFAULT 'DRAFT' | 状态：DRAFT / ACTIVE / DISABLED |
| sync_mode | VARCHAR(16) NOT NULL DEFAULT 'MANUAL' | 同步模式：MANUAL / INCREMENTAL / FULL |
| cron_expression | VARCHAR(64) | 定时调度 Cron 表达式 |
| last_run_time | TIMESTAMP | 最后执行时间 |
| created_by | VARCHAR(64) NOT NULL | 创建人 |
| created_at | TIMESTAMP NOT NULL DEFAULT NOW() | 创建时间 |
| updated_by | VARCHAR(64) | 更新人 |
| updated_at | TIMESTAMP | 更新时间 |
| is_deleted | BOOLEAN NOT NULL DEFAULT FALSE | 软删除 |
| org_id | BIGINT NOT NULL | 组织ID |

#### r_etl_step（管道步骤）

| 列名 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 主键 |
| pipeline_id | BIGINT NOT NULL | 所属管道 |
| step_name | VARCHAR(128) NOT NULL | 步骤名称 |
| step_order | INT NOT NULL | 执行顺序 |
| step_type | VARCHAR(16) NOT NULL | 映射类型：ONE_TO_ONE / ONE_TO_MANY / MANY_TO_ONE |
| source_schema | VARCHAR(32) | 源 schema |
| source_table | VARCHAR(128) NOT NULL | 源表名 |
| target_schema | VARCHAR(32) | 目标 schema |
| target_table | VARCHAR(128) NOT NULL | 目标表名 |
| join_config | JSONB | 多源表 JOIN 配置 |
| filter_condition | TEXT | WHERE 过滤条件 |
| transform_config | JSONB | 聚合/拆分规则 |
| pre_sql | TEXT | 步骤前执行的 SQL |
| post_sql | TEXT | 步骤后执行的 SQL |
| on_error | VARCHAR(16) NOT NULL DEFAULT 'ABORT' | 错误处理：SKIP / RETRY / ABORT |
| sync_mode | VARCHAR(16) NOT NULL DEFAULT 'INCREMENTAL' | 步骤级同步模式 |
| last_sync_time | TIMESTAMP | 步骤最后同步时间 |
| created_by | VARCHAR(64) NOT NULL | |
| created_at | TIMESTAMP NOT NULL DEFAULT NOW() | |
| updated_by | VARCHAR(64) | |
| updated_at | TIMESTAMP | |
| is_deleted | BOOLEAN NOT NULL DEFAULT FALSE | |
| org_id | BIGINT NOT NULL | |

#### r_etl_field_mapping（字段映射）

| 列名 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 主键 |
| step_id | BIGINT NOT NULL | 所属步骤 |
| source_column | VARCHAR(64) | 源字段名 |
| source_table_alias | VARCHAR(32) | 多源表时的别名 |
| target_column | VARCHAR(64) NOT NULL | 目标字段名 |
| transform_type | VARCHAR(16) NOT NULL DEFAULT 'DIRECT' | 转换类型 |
| transform_expr | TEXT | 转换表达式 |
| default_value | TEXT | 默认值 |
| is_required | BOOLEAN NOT NULL DEFAULT FALSE | 是否必填 |
| sort_order | INT | 排序 |
| created_by | VARCHAR(64) NOT NULL | |
| created_at | TIMESTAMP NOT NULL DEFAULT NOW() | |
| updated_by | VARCHAR(64) | |
| updated_at | TIMESTAMP | |
| is_deleted | BOOLEAN NOT NULL DEFAULT FALSE | |
| org_id | BIGINT NOT NULL | |

**transform_type 枚举值**：
- `DIRECT`：直接映射，无需转换
- `MAP`：值映射（如 M→男, F→女），映射规则存于 transform_expr（JSON 格式）
- `EXPRESSION`：表达式转换（如字符串拼接、数学计算）
- `CONSTANT`：固定常量值
- `DATE_FMT`：日期格式转换
- `LOOKUP`：关联查询（从另一张表查找值）

#### r_etl_execution（执行记录）

| 列名 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 主键 |
| pipeline_id | BIGINT NOT NULL | 关联管道 |
| step_id | BIGINT | 关联步骤（为空时为管道级执行） |
| status | VARCHAR(16) NOT NULL DEFAULT 'PENDING' | 状态：PENDING / RUNNING / SUCCESS / FAILED / CANCELLED / SKIPPED |
| engine_config | TEXT | 实际生成的引擎配置（Embulk YAML 等） |
| start_time | TIMESTAMP | 开始时间 |
| end_time | TIMESTAMP | 结束时间 |
| rows_read | BIGINT DEFAULT 0 | 读取行数 |
| rows_written | BIGINT DEFAULT 0 | 写入行数 |
| rows_skipped | BIGINT DEFAULT 0 | 跳过行数 |
| rows_error | BIGINT DEFAULT 0 | 错误行数 |
| error_message | TEXT | 错误信息 |
| log_path | VARCHAR(256) | 日志文件路径 |
| trigger_type | VARCHAR(16) NOT NULL DEFAULT 'MANUAL' | 触发方式：MANUAL / SCHEDULE / RETRY |
| execution_snapshot | JSONB | 执行时的映射配置快照 |
| created_by | VARCHAR(64) NOT NULL | |
| created_at | TIMESTAMP NOT NULL DEFAULT NOW() | |
| updated_by | VARCHAR(64) | |
| updated_at | TIMESTAMP | |
| is_deleted | BOOLEAN NOT NULL DEFAULT FALSE | |
| org_id | BIGINT NOT NULL | |

## 3. ETL 引擎集成层

### 3.1 可插拔引擎架构

```
EtlEngine (接口)
  ├── EmbulkEtlEngine (默认实现)
  ├── SparkEtlEngine (未来扩展)
  └── PythonEtlEngine (未来扩展)
```

**`EtlEngine` 接口定义**：

```java
public interface EtlEngine {
    String getEngineType();
    String generateConfig(EtlStep step, List<FieldMapping> mappings);
    EtlExecutionResult execute(String config, Map<String, String> params);
    EtlExecutionStatus getStatus(String executionId);
    boolean cancel(String executionId);
}
```

### 3.2 Embulk 集成方式

- **配置生成**：根据 `r_etl_step` + `r_etl_field_mapping` 自动生成 Embulk YAML 配置
- **执行方式**：Java ProcessBuilder 调用 `embulk run <config.yml>`
- **输出捕获**：重定向 stdout/stderr 到日志文件，解析 Embulk 输出获取行数统计
- **状态跟踪**：轮询进程存活状态 + 解析日志中的进度信息
- **增量同步**：通过 Embulk merge 模式 + `WHERE` 条件实现

### 3.3 Embulk 配置生成示例

对于 `o3_patients → c_patient` 映射：

```yaml
in:
  type: postgresql
  host: ${source.host}
  port: ${source.port}
  database: ${source.database}
  user: ${source.user}
  password: ${source.password}
  query: >
    SELECT subject_id, gender, dob, expire_flag, ...
    FROM ods.o3_patients
    WHERE _loaded_at > '{{ last_sync_time }}'
out:
  type: postgresql
  host: ${target.host}
  database: maidc
  table: cdr.c_patient
  mode: merge
  merge_keys: [source_id, org_id]
  column_options:
    source_system: {value: "MIMIC3"}
    source_id: {value_from: subject_id, to_type: varchar}
    gender: {value_from: gender, to_type: char}
```

## 4. 后端 API 设计

### 4.1 管道管理

**`/api/v1/cdr/etl/pipelines`**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 列出管道（分页+过滤） |
| POST | `/` | 创建管道 |
| GET | `/{id}` | 管道详情（含步骤列表） |
| PUT | `/{id}` | 更新管道 |
| DELETE | `/{id}` | 删除管道 |
| POST | `/{id}/run` | 手动触发执行 |
| POST | `/{id}/validate` | 校验管道配置完整性 |
| GET | `/{id}/executions` | 管道的执行历史 |

### 4.2 步骤管理

**`/api/v1/cdr/etl/pipelines/{pipelineId}/steps`**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 列出步骤（按 step_order 排序） |
| POST | `/` | 添加步骤 |
| PUT | `/{stepId}` | 更新步骤 |
| DELETE | `/{stepId}` | 删除步骤 |
| PUT | `/reorder` | 批量调整步骤顺序 |
| POST | `/{stepId}/preview` | 预览映射结果（取前10条源数据模拟转换） |

### 4.3 字段映射

**`/api/v1/cdr/etl/steps/{stepId}/field-mappings`**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 列出字段映射 |
| PUT | `/` | 批量更新字段映射（整体替换） |
| POST | `/auto-map` | 自动映射（按字段名相似度匹配） |

### 4.4 执行记录

**`/api/v1/cdr/etl/executions`**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 列出所有执行记录 |
| GET | `/{id}` | 执行详情 |
| GET | `/{id}/logs` | 执行日志 |
| POST | `/{id}/cancel` | 取消执行 |
| POST | `/{id}/retry` | 重试失败的执行 |
| GET | `/{id}/progress` | 实时进度（SSE 推送） |

### 4.5 元数据查询

**`/api/v1/cdr/etl/metadata`**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/schemas` | 列出所有 schema |
| GET | `/schemas/{schema}/tables` | 列出 schema 下的表 |
| GET | `/tables/{schema}.{table}/columns` | 列出表的字段信息 |
| GET | `/source-tables/{sourceId}` | 查询外部数据源的表和字段 |

### 4.6 服务层

| 服务 | 职责 |
|------|------|
| `EtlPipelineService` | 管道 CRUD + 校验 |
| `EtlStepService` | 步骤管理 + 顺序编排 |
| `EtlFieldMappingService` | 字段映射管理 + 自动映射算法 |
| `EtlExecutionService` | 执行调度、引擎调用、状态跟踪 |
| `EtlConfigGenerator` | 根据步骤+映射生成引擎配置（Embulk YAML） |
| `EtlMetadataService` | 查询数据库 schema/table/column 元信息 |

## 5. 前端页面设计

### 5.1 菜单结构

在现有 **数据管理** 菜单下新增：

```
数据管理
├── 患者管理        (已有)
├── 就诊记录        (已有)
├── ...
├── 数据源管理      (已有 DataSourceList)
├── ETL 管道管理    (新增)
├── 执行监控        (改造 SyncTaskList)
```

### 5.2 页面 1：ETL 管道列表 (`EtlPipelineList.vue`)

标准列表页（PageContainer + SearchForm + Table）。

**表格列**：
- 管道名称（pipeline_name）
- 数据源（关联显示 source_name）
- 引擎类型（Embulk / Spark / Python 标签）
- 步骤数（关联统计 step_count）
- 状态（DRAFT 灰 / ACTIVE 绿 / DISABLED 红）
- 最后执行时间 + 状态标签
- 操作（编辑、执行、复制、删除）

**操作**：
- 新建管道 → 进入管道配置页
- 执行 → 弹窗确认后触发，跳转执行监控
- 复制 → 基于现有管道创建副本
- 校验 → 检查配置完整性

### 5.3 页面 2：管道配置页 (`EtlPipelineConfig.vue`) -- 核心

采用步骤卡片 + 字段映射表布局。

**页面结构**：

1. **头部区域**：管道名称、数据源选择、引擎类型、描述
2. **步骤编排区**：横向卡片流，显示所有步骤（可拖拽排序），支持添加/删除步骤
3. **步骤配置面板**（点击步骤卡片展开）：
   - 源表/目标表选择（下拉，从元数据 API 加载）
   - 映射类型选择（1:1 / 1:N / N:1）
   - 错误处理策略
   - **字段映射表**（核心交互）：
     - 源字段列（下拉选择）
     - 转换规则列（点击弹出配置面板）
     - 目标字段列（下拉选择）
     - 操作列（删除）
   - 工具栏：自动映射、添加行、清空
4. **高级配置**：前置 SQL、后置 SQL（可折叠）
5. **底部操作**：保存步骤、预览数据

**转换规则配置面板**（根据 transform_type 渲染不同 UI）：
- `DIRECT`：无需配置
- `MAP`：弹出键值对编辑器（如 M→男, F→女）
- `EXPRESSION`：文本表达式编辑器
- `CONSTANT`：填入固定值
- `DATE_FMT`：选择日期格式模板
- `LOOKUP`：配置关联查询表和条件

**自动映射算法**：
1. 精确匹配字段名（不区分大小写）
2. 模糊匹配（编辑距离 ≤ 2）
3. 按字段类型匹配（同类型未映射字段配对）
4. 常见映射规则（如 name ↔ patient_name）

### 5.4 页面 3：执行监控 (`EtlExecutionList.vue`)

基于现有 `SyncTaskList.vue` 改造。

**增强内容**：
- 增加管道名称、步骤名称筛选
- 进度百分比显示（rows_written / rows_read）
- 引擎类型标签
- 执行记录详情：映射配置快照 + 日志 + 错误行详情
- 单步骤重试

## 6. 执行流程

### 6.1 完整执行流程

```
用户点击"执行"
  → EtlExecutionService 创建 r_etl_execution(status=PENDING)
  → 按 step_order 顺序处理每个步骤：
      1. EtlConfigGenerator.generateConfig(step, mappings)
         生成 Embulk YAML 配置
      2. EmbulkEtlEngine.execute(config, params)
         ProcessBuilder 调用 embulk run
      3. 轮询进程状态，更新 rows_read / rows_written
      4. 进程结束 → 记录结果
      5. 检查 on_error 策略，决定是否继续
  → 全部完成 → 更新管道 last_run_time
  → 通过 maidc-msg 发送通知
```

### 6.2 步骤依赖与错误处理

- 严格按 `step_order` 顺序执行
- 前一步骤失败时根据 `on_error` 决策：
  - `ABORT`：中止整个管道，标记后续步骤为 SKIPPED
  - `SKIP`：跳过当前步骤，继续下一步
  - `RETRY`：自动重试 3 次，仍失败则 ABORT

### 6.3 增量同步策略

- 每个步骤记录 `last_sync_time`
- 生成 SQL 附加 `WHERE` 条件实现增量
- 支持 `FULL`（全量）和 `INCREMENTAL`（增量）两种模式
- Embulk `merge` 模式天然支持 upsert

### 6.4 错误处理矩阵

| 场景 | 处理方式 |
|------|---------|
| Embulk 进程崩溃 | 检测进程退出码，标记 FAILED，记录 stderr |
| 源库连接失败 | 预检阶段检测，返回明确错误信息 |
| 字段类型不匹配 | 生成配置时校验，运行时 Embulk 报错记录到 rows_error |
| 目标表约束冲突 | Embulk merge 模式处理，或记录跳过行 |
| 步骤间数据不一致 | 前置步骤失败则后续步骤不执行 |

### 6.5 消息通知

集成 `maidc-msg` 服务：
- 管道执行完成 → 通知创建者
- 步骤失败 → 通知管理员
- 长时间运行（>30min）→ 发送警告
