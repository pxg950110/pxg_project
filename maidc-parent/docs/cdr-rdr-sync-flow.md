# CDR → RDR 数据同步流程

## 数据架构概览

MAIDC 系统采用 **5 Schema 分层架构**：

| Schema | 用途 | 核心内容 |
|--------|------|----------|
| `system` | 系统管理 | 用户/角色/权限/字典/配置 |
| `cdr` | 临床数据仓库 | 患者/就诊/检验/影像/数据源/同步任务/脱敏规则 |
| `rdr` | 研究数据仓库 | 项目/队列/数据集/ETL任务/质量规则 |
| `model` | 模型管理 | 注册/版本/评估/部署/监控 |
| `audit` | 审计日志 | 操作/数据访问/系统事件 |

## 整体流程图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        外部数据源 (External Sources)                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │  HIS系统  │  │  LIS系统  │  │  PACS   │  │ MIMIC-III│               │
│  │ (MySQL)  │  │(Postgres) │  │ (DICOM) │  │ MIMIC-IV │               │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘               │
│       │              │              │              │                     │
└───────┼──────────────┼──────────────┼──────────────┼─────────────────────┘
        │              │              │              │
        ▼              ▼              ▼              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                  ① 数据源注册 & 连接管理  (CDR Schema)                    │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────┐          │
│  │                 r_data_source (CDR)                        │          │
│  │  source_name | source_type | host | port | db | status    │          │
│  └───────────────────────────────────────────────────────────┘          │
│                    │ test-connection                                     │
│                    │ schema-mapping                                      │
└────────────────────┼────────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                  ② 数据同步任务 (Sync Task)                              │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────┐          │
│  │                 r_sync_task (CDR)                          │          │
│  │  source_id | task_name | sync_type | status                │          │
│  │  total_records | success_records | failed_records          │          │
│  └───────────────────────────────────────────────────────────┘          │
│                                                                         │
│  sync_type:  FULL（全量同步） / INCREMENTAL（增量同步）                     │
│  status:     PENDING → RUNNING → SUCCESS / FAILED                        │
│  retry:      失败任务可重试                                               │
└────────────────────┬────────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                  ③ ETL 数据加工 (Extract → Transform → Load)             │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────┐          │
│  │                 r_etl_task (RDR)                           │          │
│  │  task_name | source_type | target_type | cron_expr         │          │
│  │  config(jsonb) | status: IDLE/RUNNING/PAUSED              │          │
│  └───────────────────────────────────────────────────────────┘          │
│                                                                         │
│  ┌─────────────────────┐     ┌─────────────────────┐                    │
│  │  r_etl_task_log      │     │  执行指标            │                    │
│  │  task_id | status    │     │  records_read       │                    │
│  │  start_time | end    │     │  records_written    │                    │
│  │  error_msg           │     │  error_count        │                    │
│  └─────────────────────┘     └─────────────────────┘                    │
│                                                                         │
│  ETL 加工内容:                                                           │
│  · 字段映射: 临床编码 → 标准编码 (ICD-10, LOINC)                          │
│  · 数据清洗: 去重、空值处理、格式统一                                      │
│  · 脱敏处理: 患者 PHI 信息脱敏 (r_desensitize_rule)                       │
│  · 结构转换: 扁平表 → OMOP CDM / FHIR 格式                               │
└────────────────────┬────────────────────────────────────────────────────┘
                     │
          ┌──────────┴──────────┐
          ▼                     ▼
┌─────────────────┐   ┌─────────────────────────────────────────────────┐
│  ④ 质量检测      │   │  ⑤ 入库 (RDR Schema)                            │
│                 │   │                                                  │
│ r_data_quality  │   │  ┌────────────────┐  ┌────────────────┐        │
│   _rule (RDR)   │   │  │ 研究项目        │  │ 数据集          │        │
│  rule_name      │   │  │ r_project      │  │ r_dataset      │        │
│  rule_type      │   │  └────────────────┘  └────────────────┘        │
│  rule_expr      │   │  ┌────────────────┐  ┌────────────────┐        │
│  threshold      │   │  │ 研究队列        │  │ 研究对象        │        │
│  severity       │   │  │ r_research_    │  │ r_study_       │        │
│                 │   │  │ cohort         │  │ subject        │        │
│ r_data_quality  │   │  └────────────────┘  └────────────────┘        │
│   _result (RDR) │   │  ┌────────────────┐  ┌────────────────┐        │
│  total_records  │   │  │ 影像数据集      │  │ 基因数据集      │        │
│  pass_count     │   │  │ r_imaging_     │  │ r_genomic_     │        │
│  pass_rate      │   │  │ dataset        │  │ dataset        │        │
│  result_detail  │   │  └────────────────┘  └────────────────┘        │
│                 │   │  ┌────────────────┐  ┌────────────────┐        │
│ 检测类型:        │   │  │ 文本数据集      │  │ 临床特征        │        │
│ · 完整性检测     │   │  │ r_text_dataset │  │ r_clinical_    │        │
│ · 一致性检测     │   │  └────────────────┘  │ feature        │        │
│ · 唯一性检测     │   │                      └────────────────┘        │
│ · 范围检测      │   │                                                  │
│ · 自定义规则     │   │                                                  │
└────────┬────────┘   └──────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     ⑥ 数据脱敏 (CDR Schema)                              │
│                                                                         │
│  r_desensitize_rule                                                     │
│  ┌────────────┬────────────┬──────────────────────────────────┐        │
│  │ 字段类型    │ 脱敏策略    │ 效果                              │        │
│  ├────────────┼────────────┼──────────────────────────────────┤        │
│  │ PHONE      │ MASK       │ 138****5678                      │        │
│  │ ID_CARD    │ MASK       │ 3101****1234                     │        │
│  │ NAME       │ REPLACE    │ 张**                             │        │
│  │ EMAIL      │ MASK       │ z**@example.com                  │        │
│  │ ADDRESS    │ DELETE     │ (移除)                            │        │
│  │ CUSTOM     │ HASH       │ 哈希值                            │        │
│  └────────────┴────────────┴──────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────────────────┘
```

## 阶段说明

### ① 数据源注册 & 连接管理

- **Schema**: CDR
- **核心表**: `r_data_source`
- **功能**:
  - 注册外部临床系统（HIS/LIS/PACS）的连接信息
  - 测试数据库连通性 (`test-connection`)
  - 查看 Schema 映射关系 (`schema-mapping`)
  - 查看数据统计 (`statistics`)

### ② 数据同步

- **Schema**: CDR
- **核心表**: `r_sync_task`
- **同步模式**:
  - `FULL` — 全量同步，拉取源系统全部数据
  - `INCREMENTAL` — 增量同步，仅拉取变更数据
- **任务状态流转**: `PENDING → RUNNING → SUCCESS / FAILED`
- **失败处理**: 记录 `error_msg`，支持 `retry` 重试
- **统计指标**: `total_records` / `success_records` / `failed_records`

### ③ ETL 数据加工

- **Schema**: RDR
- **核心表**: `r_etl_task` + `r_etl_task_log`
- **调度方式**: Cron 表达式定时调度 + 手动触发
- **任务状态**: `IDLE → RUNNING → PAUSED`
- **加工内容**:
  1. **字段映射**: 临床编码 → 标准编码（ICD-10, LOINC, SNOMED CT）
  2. **数据清洗**: 去重、空值处理、格式统一
  3. **脱敏处理**: 患者 PHI 信息脱敏
  4. **结构转换**: 扁平表 → OMOP CDM / FHIR 格式

### ④ 质量检测

- **Schema**: RDR
- **核心表**: `r_data_quality_rule` + `r_data_quality_result`
- **规则类型**:
  - `COMPLETENESS` — 完整性检测（必填字段非空）
  - `CONSISTENCY` — 一致性检测（关联数据匹配）
  - `UNIQUENESS` — 唯一性检测（主键/唯一约束）
  - `RANGE` — 范围检测（数值/日期合理范围）
  - `CUSTOM` — 自定义规则（JSON 表达式）
- **规则配置**: `rule_expr` (JSONB) 定义检测逻辑，`threshold` 设置通过阈值
- **严重级别**: `INFO / WARN / ERROR / CRITICAL`
- **检测结果**: 记录 `total_records`、`pass_count`、`pass_rate`

### ⑤ 数据入库 (RDR)

- **Schema**: RDR
- **入库目标**:

| 目标表 | 说明 |
|--------|------|
| `r_project` | 研究项目（PI、成员、状态） |
| `r_dataset` / `r_dataset_version` | 数据集（版本化管理） |
| `r_research_cohort` | 研究队列（入排标准筛选） |
| `r_study_subject` | 研究对象（匿名化 ID） |
| `r_imaging_dataset` | 影像数据集 |
| `r_genomic_dataset` | 基因数据集 |
| `r_text_dataset` | 文本数据集（NLP） |
| `r_clinical_feature` | 临床特征（结构化特征表） |

### ⑥ 数据脱敏

- **Schema**: CDR
- **核心表**: `r_desensitize_rule`
- **脱敏策略**:

| 策略 | 说明 | 示例 |
|------|------|------|
| `MASK` | 遮盖部分字符 | `138****5678` |
| `HASH` | 哈希替换 | `a1b2c3d4` |
| `REPLACE` | 替换为占位符 | `张**` |
| `DELETE` | 直接移除 | _(空)_ |
| `ENCRYPT` | 可逆加密 | AES 加密值 |

- **字段类型**: `PHONE` / `ID_CARD` / `NAME` / `EMAIL` / `ADDRESS` / `CUSTOM`
- **支持预览**: `/desensitize-rules/preview` 接口可预览脱敏效果

## API 映射

| 前端页面 | 前端路径 | 后端 Controller | 端口 |
|----------|----------|----------------|------|
| 数据源管理 | `/cdr/datasources` | `DataSourceController` | 8082 |
| 数据同步 | `/cdr/sync-tasks` | `SyncTaskController` | 8082 |
| ETL 任务 | `/etl/tasks` | `EtlController` | 8082 |
| 质量规则 | `/rdr/quality-rules` | `RdrController` | 8082 |
| 质量检测 | `/rdr/quality-results` | `RdrController` | 8082 |
| 脱敏规则 | `/cdr/desensitize-rules` | `DesensitizeRuleController` | 8082 |
