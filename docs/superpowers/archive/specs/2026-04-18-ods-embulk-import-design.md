# ODS 数据导入设计 — Embulk ETL

## 概述

将 `data/icu-datasets/` 下的 MIMIC-III（~44GB，26 张表）和 MIMIC-IV（~91GB，31 张表）CSV 数据通过 Embulk 导入 PostgreSQL `ods` schema。Spring Boot 动态生成 YAML 配置，ProcessBuilder 调 Embulk CLI 执行，导入后校验行数一致性。

## 1. 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│  maidc-data (Spring Boot 8082)                                  │
│                                                                 │
│  EtlImportController                                           │
│    POST /api/v1/etl/import/start       ← 触发导入              │
│    GET  /api/v1/etl/import/status      ← 查询进度              │
│    POST /api/v1/etl/import/retry/{id}  ← 失败重试              │
│                                                                 │
│  OdsImportService                                              │
│    1. 扫描 data/icu-datasets/ CSV 清单                         │
│    2. 为每张表生成 Embulk YAML 配置（含列映射）                 │
│    3. 线程池提交：小表串行，大表并行（max 3 进程）              │
│    4. ProcessBuilder 调用 embulk run xxx.yml                   │
│    5. 每表完成后校验行数 → ods_import_check                    │
│    6. 全部状态写入 ods_import_log                               │
│                                                                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐        │
│  │ Embulk CLI  │    │ Embulk CLI  │    │ Embulk CLI  │ ...    │
│  │ o3_patients │    │ o4_patients │    │ o3_admissns │        │
│  │ .yml        │    │ .yml        │    │ .yml        │        │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘        │
│         │ COPY             │ COPY             │ COPY           │
└─────────┼──────────────────┼──────────────────┼───────────────┘
          ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────────┐
│  PostgreSQL (ods schema)                                        │
│    o3_* 26张 (MIMIC-III)    o4_* 31张 (MIMIC-IV)              │
│    ods_import_log            ods_import_check                   │
└─────────────────────────────────────────────────────────────────┘
```

**技术选型理由**：Embulk CLI + ProcessBuilder，不用嵌入式 API。Spring Boot 动态生成 YAML，ProcessBuilder 调 Embulk 进程执行，官方推荐方式，稳定可靠。

## 2. 元数据表

### 2.1 导入进度表

```sql
CREATE TABLE ods.ods_import_log (
    id              BIGSERIAL   PRIMARY KEY,
    table_name      VARCHAR(64) NOT NULL,
    source_file     VARCHAR(256) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING',  -- PENDING/RUNNING/SUCCESS/FAILED
    csv_rows        BIGINT,
    db_rows         BIGINT,
    row_match       BOOLEAN,
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    duration_sec    INT,
    error_msg       TEXT,
    batch_id        VARCHAR(32) NOT NULL
);
```

### 2.2 校验结果表

```sql
CREATE TABLE ods.ods_import_check (
    id              BIGSERIAL   PRIMARY KEY,
    batch_id        VARCHAR(32) NOT NULL,
    table_name      VARCHAR(64) NOT NULL,
    check_type      VARCHAR(16) NOT NULL,       -- ROW_COUNT
    check_result    VARCHAR(8) NOT NULL,        -- PASS / FAIL
    expected        BIGINT,
    actual          BIGINT,
    diff            BIGINT,
    checked_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);
```

## 3. Embulk 配置

### 3.1 YAML 配置模板（以 o3_patients 为例）

```yaml
in:
  type: file
  path_prefix: "E:/pxg_project/data/icu-datasets/iii/PATIENTS.csv/PATIENTS"
  parser:
    type: csv
    delimiter: ","
    quote: '"'
    header_line: true
    columns:
      - {name: row_id, type: long}
      - {name: subject_id, type: long}
      - {name: gender, type: string}
      - {name: dob, type: timestamp, format: "%Y-%m-%d %H:%M:%S"}
      - {name: dod, type: timestamp, format: "%Y-%m-%d %H:%M:%S"}
      - {name: dod_hosp, type: timestamp, format: "%Y-%m-%d %H:%M:%S"}
      - {name: dod_ssn, type: timestamp, format: "%Y-%m-%d %H:%M:%S"}
      - {name: expire_flag, type: long}
filters:
  - type: add_columns
    columns:
      - {name: _batch_id, value: "batch_20260418_001"}
      - {name: _source_file, value: "PATIENTS.csv"}
      - {name: "_loaded_at", value: "2026-04-18T00:00:00", type: timestamp}
out:
  type: postgresql
  host: localhost
  port: 5432
  user: maidc
  password: maidc123
  database: maidc
  schema: ods
  table: o3_patients
  mode: insert
```

### 3.2 列映射策略

| CSV 来源 | 映射规则 | 示例 |
|----------|----------|------|
| MIMIC-III CSV 列名 | 小写 + 去 `"` | `ROW_ID` → `row_id` |
| MIMIC-IV CSV 列名 | 直接使用小写 | `subject_id` → `subject_id` |
| ODS 表内置列 | `_batch_id`, `_source_file`, `_loaded_at`, `_row_hash`, `_is_valid` 由 filter 注入 |

## 4. 导入执行流程

### 4.1 表分类

| 分类 | 标准 | MIMIC-III | MIMIC-IV | 执行方式 |
|------|------|-----------|----------|----------|
| 小表 | < 500MB | 18 张 | 20 张 | 线程池串行，每个一 Embulk 进程 |
| 大表 | ≥ 500MB | 8 张 | 11 张 | 线程池并行，max 3 个 Embulk 进程同时运行 |

### 4.2 执行步骤

```
POST /api/v1/etl/import/start
        │
        ▼
  1. 生成 batch_id (时间戳)
  2. 创建 ODS schema + 建表 (07/08/09 SQL)
  3. 创建月分区 (大表)
  4. 创建 ods_import_log / ods_import_check
  5. 扫描 CSV → 生成 57 条 import_log (PENDING)
  6. 线程池提交任务 (core=3, max=3)
     │
     ├─ Thread-1: o3_patients.yml → embulk run → 校验行数
     ├─ Thread-2: o4_patients.yml → embulk run → 校验行数
     └─ Thread-3: o3_admissions.yml → embulk run → 校验行数
        │
        ▼
  7. 每表完成后:
     - wc -l CSV → csv_rows
     - SELECT COUNT(*) → db_rows
     - 对比 → ods_import_check (PASS/FAIL)
     - 更新 ods_import_log (status/duration)
        │
        ▼
  8. 全部完成 → 汇总报告
     总表数 / 成功 / 失败 / 总耗时
```

### 4.3 大表策略

大表（如 `o3_chartevents` 33GB）不按月拆分 Embulk 任务——直接交给 Embulk 的 `COPY` 协议处理。Embulk PostgreSQL output plugin 内部使用 COPY 协议，速度约 50MB/s，单表单任务即可。

### 4.4 进度查询 API

```
GET /api/v1/etl/import/status

返回:
{
  "batchId": "batch_20260418_001",
  "total": 57,
  "success": 30,
  "failed": 2,
  "running": 1,
  "pending": 24,
  "tables": [
    {"table": "o3_patients", "status": "SUCCESS", "csvRows": 46520, "dbRows": 46520, "match": true, "duration": 12},
    {"table": "o3_chartevents", "status": "RUNNING", "startedAt": "2026-04-18T10:30:00"},
    {"table": "o4_labevents", "status": "FAILED", "error": "..."}
  ]
}
```

### 4.5 失败重试

```
POST /api/v1/etl/import/retry/{tableName}

- 清空该表数据 (DELETE FROM ods.{table} WHERE _batch_id = ?)
- 重新生成 YAML 配置
- embulk run
- 校验行数
```

## 5. 代码结构

### 5.1 新增文件

```
maidc-data/src/main/java/com/maidc/data/
├── controller/
│   └── EtlImportController.java          ← 导入 API
├── service/
│   ├── OdsImportService.java             ← 核心编排：扫描/生成YAML/调度/校验
│   └── OdsImportRepository.java          ← import_log/check 的 JDBC 操作
├── dto/
│   ├── ImportTask.java                   ← 单表导入任务定义
│   └── ImportStatusVO.java              ← 状态查询返回
└── etl/
    ├── EmbulkConfigGenerator.java        ← 动态生成 YAML 配置
    ├── EmbulkRunner.java                 ← ProcessBuilder 封装，日志捕获
    └── TableMapping.java                 ← 57 张表的 CSV路径→表名→列映射定义

scripts/
└── install-embulk.sh                     ← Embulk 安装脚本
```

### 5.2 Embulk 安装

```bash
# scripts/install-embulk.sh
curl -L https://github.com/embulk/embulk/releases/download/v0.11.5/embulk-0.11.5.jar -o scripts/embulk.jar
java -jar scripts/embulk.jar gem install embulk-output-postgresql
```

### 5.3 Maven 依赖

`maidc-data` 的 `pom.xml` **无需新增依赖**。所有逻辑用标准 JDK 实现。

### 5.4 配置项

```yaml
# application-dev.yml
maidc:
  etl:
    embulk-path: scripts/embulk.jar
    csv-base-dir: E:/pxg_project/data/icu-datasets
    parallel: 3
    batch-size: 10000
    db-url: jdbc:postgresql://localhost:5432/maidc
    db-schema: ods
    db-user: maidc
    db-password: maidc123
```

## 6. PostgreSQL 调优

### 6.1 导入前临时调优

```sql
ALTER SYSTEM SET shared_buffers = '1GB';
ALTER SYSTEM SET work_mem = '256MB';
ALTER SYSTEM SET maintenance_work_mem = '512MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '64MB';
SELECT pg_reload_conf();
```

### 6.2 导入前操作顺序

```
1. 创建 ODS schema + 建表 (执行 07/08/09 SQL)
2. 创建月分区 (大表)
3. 创建 ods_import_log / ods_import_check
4. 暂停索引创建（大表导入后再建索引更快）
5. 调大 PG 参数
6. 开始 Embulk 导入
7. 导入完成后建索引 + ANALYZE
8. 恢复 PG 参数
```

### 6.3 预估耗时

| 数据集 | 总大小 | 预估速度 | 预估耗时 |
|--------|--------|----------|----------|
| MIMIC-III 小表 (18张) | ~5GB | ~50MB/s | ~2 分钟 |
| MIMIC-III 大表 (8张) | ~39GB | ~50MB/s × 3并行 | ~4 分钟 |
| MIMIC-IV 小表 (20张) | ~15GB | ~50MB/s | ~5 分钟 |
| MIMIC-IV 大表 (11张) | ~76GB | ~50MB/s × 3并行 | ~8 分钟 |
| 校验 (57张) | — | — | ~3 分钟 |
| **总计** | **~135GB** | | **~25 分钟** |

实际耗时取决于磁盘 IO。
