# MAIDC - Medical AI Data Center 医疗AI数据中心

## 产品需求文档 (PRD)

> **版本**: v1.0
> **日期**: 2026-04-08
> **状态**: 设计评审通过
> **定位**: 临床+科研一体化多中心医疗AI数据中心平台
> **目标规模**: 5-20家医院，千万级记录

---

## 目录

1. [项目概述](#1-项目概述)
2. [系统架构设计](#2-系统架构设计)
3. [技术选型](#3-技术选型)
4. [数据库设计](#4-数据库设计)
5. [微服务设计](#5-微服务设计)
6. [API设计](#6-api设计)
7. [分期实施计划](#7-分期实施计划)
8. [安全与合规](#8-安全与合规)
9. [部署架构](#9-部署架构)
10. [附录](#10-附录)

---

## 1. 项目概述

### 1.1 背景与目标

随着医疗AI技术的快速发展，医院积累了大量临床数据（影像、病历、检验、基因组等），但面临以下痛点：

- **数据孤岛**: 各科室、各院区数据分散，格式不统一
- **科研数据治理困难**: 临床数据到科研数据的ETL流程缺失
- **AI模型管理混乱**: 模型版本无追踪、部署无审批、推理无监控
- **多中心协作壁垒**: 跨院数据共享与模型协作缺乏统一平台

**MAIDC（Medical AI Data Center）** 旨在构建一个**临床+科研一体化**的多中心医疗AI数据中心平台，实现：

1. 统一临床数据仓库（CDR），标准化汇聚多源异构临床数据
2. 构建研究数据仓库（RDR），支撑科研队列、特征工程与数据治理
3. 全生命周期AI模型管理（注册-训练-评估-审批-部署-监控）
4. 安全合规的多中心数据协作与模型共享

### 1.2 核心价值

| 维度 | 价值 |
|------|------|
| 临床 | 标准化临床数据治理，提升数据质量与可用性 |
| 科研 | 一站式科研数据管理，从队列定义到数据集发布 |
| AI | 模型全生命周期管理，从注册到线上推理监控 |
| 管理 | 多中心统一管控，合规审计，资源可视化 |
| 协作 | 跨院模型共享与数据协作，推动多中心研究 |

### 1.3 用户角色

| 角色 | 描述 | 核心场景 |
|------|------|----------|
| 平台管理员 | 系统运维与权限管理 | 用户管理、服务监控、系统配置 |
| 数据管理员 | 临床/科研数据治理 | 数据源接入、ETL配置、数据质量管理 |
| 研究员 | 科研项目负责人 | 队列定义、特征工程、数据集申请 |
| AI工程师 | 模型开发与部署 | 模型注册、版本管理、评估部署 |
| 临床医生 | AI辅助诊断使用者 | 模型推理调用、结果查看 |
| 审计员 | 合规审查 | 审计日志查看、数据访问追踪 |

### 1.4 设计原则

1. **微服务优先**: 松耦合、独立部署、按需扩展
2. **数据安全合规**: 等保三级要求，患者隐私保护，审计全链路追踪
3. **标准先行**: 遵循HL7 FHIR、OMOP CDM、DICOM等医疗数据标准
4. **渐进演进**: 分期交付，Phase 1聚焦核心能力（模型管理），逐步扩展
5. **开放集成**: 标准REST API，支持外部系统对接（HIS/PACS/LIS）

---

## 2. 系统架构设计

### 2.1 整体架构

采用 **Spring Cloud Alibaba 微服务 + Python AI Worker** 的混合架构：

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端层                              │
│   Web Portal (Vue3)  │  移动端  │  第三方系统 (HIS/PACS)     │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                   Spring Cloud Gateway                       │
│            (路由 / 限流 / 鉴权 / 灰度发布)                     │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                     Nacos 注册/配置中心                       │
└──┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬─────────┘
   │      │      │      │      │      │      │      │
┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──────┐
│用户 ││数据 ││模型 ││任务 ││标注 ││审计 ││消息 ││ Python  │
│权限 ││管理 ││管理 ││调度 ││服务 ││日志 ││通知 ││AI Worker│
│服务 ││服务 ││服务 ││服务 ││     ││服务 ││服务 ││  集群    │
└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘└────┬────┘
   │      │      │      │      │      │      │         │
┌──▼──────▼──────▼──────▼──────▼──────▼──────▼─────────▼─────┐
│                     基础设施层                                │
│  PostgreSQL │ Redis │ MinIO │ RabbitMQ │ XXL-JOB │ ELK      │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 架构说明

#### 2.2.1 网关层

- **Spring Cloud Gateway**: 统一入口，负责路由转发、请求限流、JWT鉴权、灰度发布
- 支持基于路径、Header、参数的多维度路由策略
- 集成全局过滤器实现请求日志、跨域处理、统一响应封装

#### 2.2.2 服务层

- **Java微服务集群** (Spring Boot 3.x + Java 17): 承载全部业务逻辑
- **Python AI Worker集群** (FastAPI + Celery): 专注AI推理、模型评估等计算密集型任务
- 服务间通过 **OpenFeign** 同步调用 + **RabbitMQ** 异步通信

#### 2.2.3 数据层

- **PostgreSQL 15+**: 单库 `maidc`，多Schema隔离（cdr/rdr/model/system/audit）
- **Redis 7+**: 会话缓存、分布式锁、热点数据缓存
- **MinIO**: 影像文件、模型文件、附件等对象存储

#### 2.2.4 异步通信

- **RabbitMQ**: 服务间异步消息，解耦模型评估、ETL任务等长时操作
- 消息模式: Direct Exchange（点对点）+ Topic Exchange（广播）
- 死信队列处理失败消息，支持重试与人工介入

### 2.3 数据流架构

```
┌──────────┐    HL7/FHIR    ┌──────────┐    ETL     ┌──────────┐
│  HIS系统  │──────────────▶│   CDR    │──────────▶│   RDR    │
│  PACS系统 │─── DICOM ────▶│ 临床数据  │           │ 研究数据  │
│  LIS系统  │─── HL7 ──────▶│  仓库     │           │  仓库     │
└──────────┘               └──────────┘           └──────────┘
                                │                      │
                                │    数据集发布          │
                                └──────────┬───────────┘
                                           │
                                           ▼
                                    ┌──────────────┐
                                    │  模型管理服务  │
                                    │  注册/训练/评估 │
                                    │  审批/部署/监控 │
                                    └──────┬───────┘
                                           │ 推理API
                                           ▼
                                    ┌──────────────┐
                                    │  临床应用     │
                                    │  辅助诊断/报告 │
                                    └──────────────┘
```

---

## 3. 技术选型

### 3.1 技术栈总览

| 层次 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **网关** | Spring Cloud Gateway | 最新稳定版 | API路由、限流、鉴权 |
| **注册/配置** | Nacos | 2.x | 服务注册发现、配置管理 |
| **服务框架** | Spring Boot | 3.x | Java业务微服务 |
| **JDK** | OpenJDK | 17 | Java运行时 |
| **AI引擎** | Python FastAPI + Celery | 3.10+ | AI推理与异步任务执行 |
| **消息队列** | RabbitMQ | 3.12+ | 服务间异步通信 |
| **数据库** | PostgreSQL | 15+ | 单库多Schema业务数据存储 |
| **缓存** | Redis | 7+ | 会话、缓存、分布式锁 |
| **对象存储** | MinIO | 最新版 | 影像/模型/附件文件存储 |
| **任务调度** | XXL-JOB | 2.4+ | 分布式定时任务 |
| **监控** | Prometheus + Grafana | 最新版 | 系统监控与可视化 |
| **日志** | ELK (Elasticsearch + Logstash + Kibana) | 8.x | 集中式日志管理 |
| **容器化** | Docker Compose → Kubernetes | - | Phase 1单机 / Phase 3+集群 |

### 3.2 选型决策说明

#### 3.2.1 为什么选择Spring Cloud Alibaba？

| 对比项 | Spring Cloud Alibaba | Spring Cloud Netflix | Quarkus |
|--------|---------------------|---------------------|---------|
| 社区活跃度 | 高（阿里维护） | 低（已停止维护） | 中 |
| 中文生态 | 优秀 | 一般 | 一般 |
| Nacos vs Eureka | 配置+注册二合一 | 仅注册 | 需第三方 |
| 企业级支持 | 阿里云商业支持 | 无 | 红帽支持 |
| 学习曲线 | 中等 | 低 | 较高 |

**结论**: Spring Cloud Alibaba生态活跃、中文文档完善、Nacos兼具注册与配置中心功能，适合国内医疗信息化团队。

#### 3.2.2 为什么选择PostgreSQL而非MySQL？

| 对比项 | PostgreSQL | MySQL |
|--------|-----------|-------|
| 分区表 | 原生声明式分区 | 需手动管理 |
| JSON支持 | JSONB索引，高效查询 | 有限支持 |
| Schema隔离 | 原生Schema，权限控制 | 需多库 |
| 扩展性 | PostGIS/pg_trgm等丰富 | 有限 |
| 分析能力 | 窗口函数、CTE优秀 | 一般 |

**结论**: PostgreSQL的Schema隔离能力直接支撑单库多Schema架构，JSONB适合医疗半结构化数据，分区表支撑推理日志等时序数据。

#### 3.2.3 为什么Python AI Worker独立部署？

- **隔离性**: AI推理（GPU密集）与业务服务（IO密集）资源隔离，互不影响
- **灵活性**: Python生态（PyTorch/TensorFlow/transformers）直接使用，无需JNI桥接
- **可扩展**: Worker节点可独立扩缩容，按GPU资源弹性调度
- **技术栈匹配**: AI工程师直接贡献Python代码，降低协作门槛

---

## 4. 数据库设计

### 4.1 总体规划

采用 **PostgreSQL 单库 `maidc`，多Schema隔离** 策略：

| Schema | 用途 | 预估表数量 |
|--------|------|-----------|
| `cdr` | 临床数据仓库 | 20+ |
| `rdr` | 研究数据仓库 | 20+ |
| `model` | 模型管理 | 11 |
| `system` | 系统管理 | 7 |
| `audit` | 审计日志 | 3 |

### 4.2 公共字段约定

所有业务表统一包含以下审计字段：

```sql
created_by   VARCHAR(64)  NOT NULL,  -- 创建人
created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),  -- 创建时间
updated_by   VARCHAR(64),             -- 更新人
updated_at   TIMESTAMP,               -- 更新时间
is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,  -- 软删除标记
org_id       BIGINT       NOT NULL,   -- 所属机构ID（多租户）
```

---

### 4.3 CDR - 临床数据仓库

临床数据仓库汇聚来自HIS、PACS、LIS等多源系统的标准化临床数据，覆盖患者就诊全流程。

#### 4.3.1 c_patient - 患者基本信息

```sql
CREATE TABLE cdr.c_patient (
    id              BIGSERIAL       PRIMARY KEY,
    patient_no      VARCHAR(32)     NOT NULL,       -- 患者编号（院内）
    id_card_no      VARCHAR(32),                    -- 身份证号（加密存储）
    name            VARCHAR(64)     NOT NULL,       -- 姓名（加密存储）
    gender          CHAR(1)         NOT NULL,       -- 性别 M/F/O
    birth_date      DATE,                           -- 出生日期
    ethnicity       VARCHAR(16),                    -- 民族
    marital_status  VARCHAR(16),                    -- 婚姻状况
    occupation      VARCHAR(32),                    -- 职业
    blood_type      VARCHAR(8),                     -- 血型 A/B/O/AB/RH+/RH-
    address         TEXT,                           -- 住址（加密存储）
    phone           VARCHAR(32),                    -- 联系电话（加密存储）
    source_system   VARCHAR(32)     NOT NULL,       -- 来源系统 HIS/PACS/LIS
    source_id       VARCHAR(64)     NOT NULL,       -- 来源系统原始ID
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_patient_org_no UNIQUE (org_id, patient_no)
);

COMMENT ON TABLE cdr.c_patient IS '患者基本信息';
CREATE INDEX idx_c_patient_source ON cdr.c_patient (source_system, source_id);
```

#### 4.3.2 c_encounter - 就诊记录

```sql
CREATE TABLE cdr.c_encounter (
    id              BIGSERIAL       PRIMARY KEY,
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    encounter_no    VARCHAR(32)     NOT NULL,       -- 就诊流水号
    encounter_type  VARCHAR(16)     NOT NULL,       -- 类型: OUTPATIENT/INPATIENT/EMERGENCY
    dept_code       VARCHAR(32)     NOT NULL,       -- 科室编码
    dept_name       VARCHAR(64)     NOT NULL,       -- 科室名称
    doctor_code     VARCHAR(32),                    -- 主治医生编码
    doctor_name     VARCHAR(64),                    -- 主治医生姓名
    admit_time      TIMESTAMP,                      -- 入院/就诊时间
    discharge_time  TIMESTAMP,                      -- 出院时间
    bed_no          VARCHAR(16),                    -- 床位号
    ward_code       VARCHAR(32),                    -- 病区编码
    diagnosis_code  VARCHAR(32),                    -- 主诊断ICD-10编码
    diagnosis_name  VARCHAR(128),                   -- 主诊断名称
    severity        VARCHAR(16),                    -- 病情分级
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_encounter_no UNIQUE (org_id, encounter_no)
);

COMMENT ON TABLE cdr.c_encounter IS '就诊记录';
CREATE INDEX idx_c_encounter_patient ON cdr.c_encounter (patient_id);
CREATE INDEX idx_c_encounter_time ON cdr.c_encounter (admit_time);
```

#### 4.3.3 c_diagnosis - 诊断记录

```sql
CREATE TABLE cdr.c_diagnosis (
    id              BIGSERIAL       PRIMARY KEY,
    encounter_id    BIGINT          NOT NULL REFERENCES cdr.c_encounter(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    diagnosis_type  VARCHAR(16)     NOT NULL,       -- MAIN/SECONDARY/ADMISSION/DISCHARGE
    icd_code        VARCHAR(32)     NOT NULL,       -- ICD-10编码
    icd_name        VARCHAR(128)    NOT NULL,       -- 诊断名称
    diagnosis_time  TIMESTAMP       NOT NULL,
    doctor_code     VARCHAR(32),
    sort_order      INT             NOT NULL DEFAULT 0,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_diagnosis IS '诊断记录';
CREATE INDEX idx_c_diagnosis_encounter ON cdr.c_diagnosis (encounter_id);
CREATE INDEX idx_c_diagnosis_icd ON cdr.c_diagnosis (icd_code);
```

#### 4.3.4 c_lab_test - 检验申请

```sql
CREATE TABLE cdr.c_lab_test (
    id              BIGSERIAL       PRIMARY KEY,
    encounter_id    BIGINT          NOT NULL REFERENCES cdr.c_encounter(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    test_no         VARCHAR(32)     NOT NULL,
    test_type       VARCHAR(32)     NOT NULL,       -- BLOOD/URINE/STOOL/CSF等
    sample_type     VARCHAR(32),
    sample_time     TIMESTAMP,
    report_time     TIMESTAMP,
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    ordering_doctor VARCHAR(64),
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_lab_test_no UNIQUE (org_id, test_no)
);

COMMENT ON TABLE cdr.c_lab_test IS '检验申请';
CREATE INDEX idx_c_lab_test_encounter ON cdr.c_lab_test (encounter_id);
```

#### 4.3.5 c_lab_panel - 检验结果明细

```sql
CREATE TABLE cdr.c_lab_panel (
    id              BIGSERIAL       PRIMARY KEY,
    lab_test_id     BIGINT          NOT NULL REFERENCES cdr.c_lab_test(id),
    encounter_id    BIGINT          NOT NULL REFERENCES cdr.c_encounter(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    item_code       VARCHAR(32)     NOT NULL,       -- LOINC编码
    item_name       VARCHAR(64)     NOT NULL,
    result_value    VARCHAR(64),
    result_unit     VARCHAR(32),
    reference_range VARCHAR(64),
    abnormal_flag   CHAR(1),                        -- N/H/L/HH/LL
    result_status   VARCHAR(16)     NOT NULL DEFAULT 'FINAL',
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_lab_panel IS '检验结果明细';
CREATE INDEX idx_c_lab_panel_test ON cdr.c_lab_panel (lab_test_id);
CREATE INDEX idx_c_lab_panel_item ON cdr.c_lab_panel (item_code);
```

#### 4.3.6 c_medication - 用药记录

```sql
CREATE TABLE cdr.c_medication (
    id              BIGSERIAL       PRIMARY KEY,
    encounter_id    BIGINT          NOT NULL REFERENCES cdr.c_encounter(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    order_no        VARCHAR(32)     NOT NULL,
    drug_code       VARCHAR(32)     NOT NULL,
    drug_name       VARCHAR(128)    NOT NULL,
    dosage          VARCHAR(32),
    dosage_unit     VARCHAR(16),
    route           VARCHAR(32),                    -- 给药途径
    frequency       VARCHAR(32),                    -- 频次
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    prescribing_doctor VARCHAR(64),
    order_type      VARCHAR(16)     NOT NULL,       -- LONG_TERM/TEMPORARY/ONCE
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_medication IS '用药记录';
CREATE INDEX idx_c_medication_encounter ON cdr.c_medication (encounter_id);
CREATE INDEX idx_c_medication_drug ON cdr.c_medication (drug_code);
```

#### 4.3.7 c_vital_sign - 生命体征

```sql
CREATE TABLE cdr.c_vital_sign (
    id              BIGSERIAL       PRIMARY KEY,
    encounter_id    BIGINT          NOT NULL REFERENCES cdr.c_encounter(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    measure_time    TIMESTAMP       NOT NULL,
    temp            DECIMAL(4,1),                   -- 体温(℃)
    heart_rate      INT,                            -- 心率(bpm)
    resp_rate       INT,                            -- 呼吸频率
    sbp             INT,                            -- 收缩压
    dbp             INT,                            -- 舒张压
    spo2            INT,                            -- 血氧饱和度(%)
    weight          DECIMAL(5,1),                   -- 体重(kg)
    height          DECIMAL(5,1),                   -- 身高(cm)
    pain_score      INT,                            -- 疼痛评分
    consciousness   VARCHAR(16),
    measure_source  VARCHAR(16)     NOT NULL DEFAULT 'MANUAL',
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_vital_sign IS '生命体征';
CREATE INDEX idx_c_vital_encounter ON cdr.c_vital_sign (encounter_id);
CREATE INDEX idx_c_vital_time ON cdr.c_vital_sign (measure_time);
```

#### 4.3.8 c_imaging_exam - 影像检查

```sql
CREATE TABLE cdr.c_imaging_exam (
    id              BIGSERIAL       PRIMARY KEY,
    encounter_id    BIGINT          NOT NULL REFERENCES cdr.c_encounter(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    accession_no    VARCHAR(32)     NOT NULL,
    exam_type       VARCHAR(32)     NOT NULL,       -- CT/MRI/XRAY/ULTRASOUND/PET_CT
    body_part       VARCHAR(32)     NOT NULL,
    exam_time       TIMESTAMP,
    report_time     TIMESTAMP,
    report_text     TEXT,
    report_doctor   VARCHAR(64),
    dicom_study_uid VARCHAR(128),
    image_count     INT,
    storage_path    VARCHAR(256),                   -- MinIO存储路径
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_imaging_accession UNIQUE (org_id, accession_no)
);

COMMENT ON TABLE cdr.c_imaging_exam IS '影像检查';
CREATE INDEX idx_c_imaging_encounter ON cdr.c_imaging_exam (encounter_id);
CREATE INDEX idx_c_imaging_dicom ON cdr.c_imaging_exam (dicom_study_uid);
```

#### 4.3.9 c_imaging_finding - 影像发现

```sql
CREATE TABLE cdr.c_imaging_finding (
    id              BIGSERIAL       PRIMARY KEY,
    imaging_exam_id BIGINT          NOT NULL REFERENCES cdr.c_imaging_exam(id),
    finding_type    VARCHAR(32)     NOT NULL,
    body_region     VARCHAR(32)     NOT NULL,
    laterality      VARCHAR(8),                     -- LEFT/RIGHT/BILATERAL
    description     TEXT,
    radiology_code  VARCHAR(32),                    -- RadLex编码
    severity        VARCHAR(16),
    bounding_box    JSONB,                          -- ROI坐标或NIfTI路径
    ai_suggestion   TEXT,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_imaging_finding IS '影像发现';
CREATE INDEX idx_c_finding_exam ON cdr.c_imaging_finding (imaging_exam_id);
```

#### 4.3.10 c_pathology - 病理记录

```sql
CREATE TABLE cdr.c_pathology (
    id              BIGSERIAL       PRIMARY KEY,
    encounter_id    BIGINT          NOT NULL REFERENCES cdr.c_encounter(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    pathology_no    VARCHAR(32)     NOT NULL,
    specimen_type   VARCHAR(32)     NOT NULL,
    specimen_source VARCHAR(64),
    collection_time TIMESTAMP,
    report_time     TIMESTAMP,
    diagnosis       TEXT,
    grade           VARCHAR(16),
    stage           VARCHAR(16),                    -- TNM分期
    ihc_result      JSONB,                          -- 免疫组化结果
    gene_result     JSONB,                          -- 基因检测结果
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_pathology_no UNIQUE (org_id, pathology_no)
);

COMMENT ON TABLE cdr.c_pathology IS '病理记录';
CREATE INDEX idx_c_pathology_encounter ON cdr.c_pathology (encounter_id);
```

#### 4.3.11 c_operation - 手术记录

```sql
CREATE TABLE cdr.c_operation (
    id              BIGSERIAL       PRIMARY KEY,
    encounter_id    BIGINT          NOT NULL REFERENCES cdr.c_encounter(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    operation_no    VARCHAR(32)     NOT NULL,
    operation_code  VARCHAR(32)     NOT NULL,       -- ICD-9-CM-3
    operation_name  VARCHAR(128)    NOT NULL,
    surgeon         VARCHAR(64),
    assistant       VARCHAR(128),
    anesthesia_type VARCHAR(32),
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    incision_type   VARCHAR(8),                     -- I/II/III
    healing_grade   VARCHAR(8),                     -- A/B/C
    intraop_note    TEXT,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_operation IS '手术记录';
CREATE INDEX idx_c_operation_encounter ON cdr.c_operation (encounter_id);
```

#### 4.3.12 c_allergy - 过敏记录

```sql
CREATE TABLE cdr.c_allergy (
    id              BIGSERIAL       PRIMARY KEY,
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    allergen_type   VARCHAR(32)     NOT NULL,       -- DRUG/FOOD/ENVIRONMENT/OTHER
    allergen_code   VARCHAR(32),
    allergen_name   VARCHAR(128)    NOT NULL,
    reaction        VARCHAR(64),
    severity        VARCHAR(16),
    confirmed_time  TIMESTAMP,
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_allergy IS '过敏记录';
CREATE INDEX idx_c_allergy_patient ON cdr.c_allergy (patient_id);
```

#### 4.3.13 c_family_history - 家族史

```sql
CREATE TABLE cdr.c_family_history (
    id              BIGSERIAL       PRIMARY KEY,
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    relationship    VARCHAR(16)     NOT NULL,
    disease_code    VARCHAR(32),
    disease_name    VARCHAR(128)    NOT NULL,
    onset_age       INT,
    is_deceased     BOOLEAN         DEFAULT FALSE,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_family_history IS '家族史';
CREATE INDEX idx_c_family_patient ON cdr.c_family_history (patient_id);
```

#### 4.3.14 c_clinical_note - 临床文书

```sql
CREATE TABLE cdr.c_clinical_note (
    id              BIGSERIAL       PRIMARY KEY,
    encounter_id    BIGINT          NOT NULL REFERENCES cdr.c_encounter(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    note_type       VARCHAR(32)     NOT NULL,       -- ADMISSION/PROGRESS/OPERATION/DISCHARGE
    title           VARCHAR(128),
    content         TEXT            NOT NULL,
    author          VARCHAR(64),
    note_time       TIMESTAMP       NOT NULL,
    is_signed       BOOLEAN         DEFAULT FALSE,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_clinical_note IS '临床文书';
CREATE INDEX idx_c_note_encounter ON cdr.c_clinical_note (encounter_id);
CREATE INDEX idx_c_note_type ON cdr.c_clinical_note (note_type);
```

#### 4.3.15 c_org - 机构信息

```sql
CREATE TABLE cdr.c_org (
    id              BIGSERIAL       PRIMARY KEY,
    org_code        VARCHAR(32)     NOT NULL,
    org_name        VARCHAR(128)    NOT NULL,
    org_type        VARCHAR(32)     NOT NULL,       -- HOSPITAL/COMMUNITY/CLINIC
    level           VARCHAR(16),                    -- 三甲/三乙/二甲等
    province        VARCHAR(32),
    city            VARCHAR(32),
    district        VARCHAR(32),
    address         VARCHAR(256),
    contact_phone   VARCHAR(32),
    his_type        VARCHAR(32),
    pacs_type       VARCHAR(32),
    connection_status VARCHAR(16)   DEFAULT 'DISCONNECTED',
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_org_code UNIQUE (org_code)
);

COMMENT ON TABLE cdr.c_org IS '机构信息';
```

#### 4.3.16 c_health_checkup - 体检记录

```sql
CREATE TABLE cdr.c_health_checkup (
    id              BIGSERIAL       PRIMARY KEY,
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    checkup_no      VARCHAR(32)     NOT NULL,
    checkup_date    DATE            NOT NULL,
    checkup_type    VARCHAR(32)     NOT NULL,       -- PERSONAL/CORPORATE/SPECIAL
    package_name    VARCHAR(128),
    overall_result  VARCHAR(16),                    -- NORMAL/ABNORMAL
    summary         TEXT,
    doctor          VARCHAR(64),
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_checkup_no UNIQUE (org_id, checkup_no)
);

COMMENT ON TABLE cdr.c_health_checkup IS '体检记录';
CREATE INDEX idx_c_checkup_patient ON cdr.c_health_checkup (patient_id);
```

#### 4.3.17 c_checkup_package - 体检套餐

```sql
CREATE TABLE cdr.c_checkup_package (
    id              BIGSERIAL       PRIMARY KEY,
    checkup_id      BIGINT          NOT NULL REFERENCES cdr.c_health_checkup(id),
    package_code    VARCHAR(32)     NOT NULL,
    package_name    VARCHAR(128)    NOT NULL,
    category        VARCHAR(32),                    -- LAB/IMAGING/PHYSICAL/OTHER
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_checkup_package IS '体检套餐';
```

#### 4.3.18 c_checkup_item_result - 体检项目结果

```sql
CREATE TABLE cdr.c_checkup_item_result (
    id              BIGSERIAL       PRIMARY KEY,
    checkup_id      BIGINT          NOT NULL REFERENCES cdr.c_health_checkup(id),
    package_id      BIGINT          REFERENCES cdr.c_checkup_package(id),
    item_code       VARCHAR(32)     NOT NULL,
    item_name       VARCHAR(64)     NOT NULL,
    result_value    VARCHAR(128),
    result_unit     VARCHAR(32),
    reference_range VARCHAR(64),
    abnormal_flag   CHAR(1),
    description     TEXT,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_checkup_item_result IS '体检项目结果';
CREATE INDEX idx_c_checkup_item ON cdr.c_checkup_item_result (checkup_id);
```

#### 4.3.19 c_checkup_summary - 体检小结

```sql
CREATE TABLE cdr.c_checkup_summary (
    id              BIGSERIAL       PRIMARY KEY,
    checkup_id      BIGINT          NOT NULL REFERENCES cdr.c_health_checkup(id),
    summary_type    VARCHAR(32)     NOT NULL,       -- DEPT/OVERALL
    dept_code       VARCHAR(32),
    summary_text    TEXT            NOT NULL,
    suggestions     TEXT,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_checkup_summary IS '体检小结';
```

#### 4.3.20 c_checkup_comparison - 体检对比

```sql
CREATE TABLE cdr.c_checkup_comparison (
    id              BIGSERIAL       PRIMARY KEY,
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    prev_checkup_id BIGINT          NOT NULL REFERENCES cdr.c_health_checkup(id),
    curr_checkup_id BIGINT          NOT NULL REFERENCES cdr.c_health_checkup(id),
    comparison_json JSONB           NOT NULL,
    trend           VARCHAR(16),                    -- IMPROVED/STABLE/WORSENED
    notes           TEXT,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_checkup_comparison IS '体检年度对比';
CREATE INDEX idx_c_checkup_comp ON cdr.c_checkup_comparison (patient_id);
```

#### 4.3.21 c_nursing_record - 护理记录

```sql
CREATE TABLE cdr.c_nursing_record (
    id              BIGSERIAL       PRIMARY KEY,
    encounter_id    BIGINT          NOT NULL REFERENCES cdr.c_encounter(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    record_type     VARCHAR(32)     NOT NULL,       -- ASSESSMENT/CARE_PLAN/IMPLEMENTATION/EVALUATION
    content         TEXT            NOT NULL,
    nurse_code      VARCHAR(32),
    nurse_name      VARCHAR(64),
    record_time     TIMESTAMP       NOT NULL,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_nursing_record IS '护理记录';
CREATE INDEX idx_c_nursing_encounter ON cdr.c_nursing_record (encounter_id);
```

#### 4.3.22 c_blood_transfusion - 输血记录

```sql
CREATE TABLE cdr.c_blood_transfusion (
    id              BIGSERIAL       PRIMARY KEY,
    encounter_id    BIGINT          NOT NULL REFERENCES cdr.c_encounter(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    blood_type      VARCHAR(8)      NOT NULL,
    blood_product   VARCHAR(32)     NOT NULL,       -- RBC/PLASMA/PLATELET
    volume          DECIMAL(6,1)    NOT NULL,
    transfusion_time TIMESTAMP      NOT NULL,
    reaction        VARCHAR(64),
    crossmatch_result VARCHAR(16),
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_blood_transfusion IS '输血记录';
```

#### 4.3.23 c_transfer - 转科记录

```sql
CREATE TABLE cdr.c_transfer (
    id              BIGSERIAL       PRIMARY KEY,
    encounter_id    BIGINT          NOT NULL REFERENCES cdr.c_encounter(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    transfer_type   VARCHAR(16)     NOT NULL,       -- IN/OUT
    from_dept       VARCHAR(32),
    from_ward       VARCHAR(32),
    to_dept         VARCHAR(32),
    to_ward         VARCHAR(32),
    transfer_time   TIMESTAMP       NOT NULL,
    reason          TEXT,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_transfer IS '转科记录';
```

#### 4.3.24 c_fee_record - 费用记录

```sql
CREATE TABLE cdr.c_fee_record (
    id              BIGSERIAL       PRIMARY KEY,
    encounter_id    BIGINT          NOT NULL REFERENCES cdr.c_encounter(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    fee_item_code   VARCHAR(32)     NOT NULL,
    fee_item_name   VARCHAR(128)    NOT NULL,
    category        VARCHAR(32)     NOT NULL,       -- DIAGNOSIS/TREATMENT/DRUG/MATERIAL
    amount          DECIMAL(12,2)   NOT NULL,
    quantity        DECIMAL(8,2)    NOT NULL DEFAULT 1,
    fee_time        TIMESTAMP       NOT NULL,
    pay_type        VARCHAR(16),                    -- INSURANCE/SELF/OTHER
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_fee_record IS '费用记录';
CREATE INDEX idx_c_fee_encounter ON cdr.c_fee_record (encounter_id);
```

#### 4.3.25 c_discharge_summary - 出院小结

```sql
CREATE TABLE cdr.c_discharge_summary (
    id              BIGSERIAL       PRIMARY KEY,
    encounter_id    BIGINT          NOT NULL REFERENCES cdr.c_encounter(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    admission_diag  TEXT,
    discharge_diag  TEXT,
    treatment_summary TEXT,
    operation_summary TEXT,
    discharge_order TEXT,
    followup_plan   TEXT,
    discharge_time  TIMESTAMP       NOT NULL,
    attending_doctor VARCHAR(64),
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_discharge_summary IS '出院小结';
CREATE INDEX idx_c_discharge_encounter ON cdr.c_discharge_summary (encounter_id);
```

#### 4.3.26 c_patient_contact - 患者联系人

```sql
CREATE TABLE cdr.c_patient_contact (
    id              BIGSERIAL       PRIMARY KEY,
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    contact_name    VARCHAR(64)     NOT NULL,
    relationship    VARCHAR(32)     NOT NULL,
    phone           VARCHAR(32),
    address         VARCHAR(256),
    is_emergency    BOOLEAN         DEFAULT FALSE,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_patient_contact IS '患者联系人';
```

#### 4.3.27 c_patient_insurance - 医保信息

```sql
CREATE TABLE cdr.c_patient_insurance (
    id              BIGSERIAL       PRIMARY KEY,
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    insurance_type  VARCHAR(32)     NOT NULL,       -- URBAN_EMPLOYEE/URBAN_RESIDENT/NEW_RURAL/COMMERCIAL
    insurance_no    VARCHAR(64)     NOT NULL,
    valid_from      DATE,
    valid_to        DATE,
    is_primary      BOOLEAN         DEFAULT TRUE,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_patient_insurance IS '医保信息';
```

#### 4.3.28 c_patient_bed - 床位记录

```sql
CREATE TABLE cdr.c_patient_bed (
    id              BIGSERIAL       PRIMARY KEY,
    encounter_id    BIGINT          NOT NULL REFERENCES cdr.c_encounter(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    bed_no          VARCHAR(16)     NOT NULL,
    ward_code       VARCHAR(32)     NOT NULL,
    ward_name       VARCHAR(64)     NOT NULL,
    room_no         VARCHAR(16),
    bed_type        VARCHAR(16),
    admit_time      TIMESTAMP       NOT NULL,
    discharge_time  TIMESTAMP,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE cdr.c_patient_bed IS '床位记录';
CREATE INDEX idx_c_bed_encounter ON cdr.c_patient_bed (encounter_id);
```

`★ Insight ─────────────────────────────────────`
**CDR设计要点**: 28张CDR表以 `c_patient` 和 `c_encounter` 为核心，采用星型结构——患者→就诊→诊疗明细（检验/影像/用药/手术等）。所有敏感字段（身份证号、姓名、电话、地址）标注"加密存储"，实际应用中使用PostgreSQL的pgcrypto扩展或应用层AES加密。`org_id` 字段贯穿所有表，支撑多租户数据隔离。
`─────────────────────────────────────────────────`

---

### 4.4 RDR - 研究数据仓库

研究数据仓库管理科研项目、研究队列、数据集及其版本化发布，支撑临床数据到科研数据的ETL转换。

#### 4.4.1 r_study_project - 研究项目

```sql
CREATE TABLE rdr.r_study_project (
    id              BIGSERIAL       PRIMARY KEY,
    project_code    VARCHAR(32)     NOT NULL,
    project_name    VARCHAR(128)    NOT NULL,
    description     TEXT,
    research_type   VARCHAR(32)     NOT NULL,       -- CLINICAL_TRIAL/OBSERVATIONAL/GENOMIC/AI
    principal_investigator VARCHAR(64) NOT NULL,     -- PI
    start_date      DATE,
    end_date        DATE,
    status          VARCHAR(16)     NOT NULL DEFAULT 'DRAFT', -- DRAFT/APPROVED/ACTIVE/SUSPENDED/COMPLETED
    ethics_approval VARCHAR(64),                    -- 伦理审批号
    funding_source  VARCHAR(128),
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_project_code UNIQUE (org_id, project_code)
);

COMMENT ON TABLE rdr.r_study_project IS '研究项目';
```

#### 4.4.2 r_study_member - 项目成员

```sql
CREATE TABLE rdr.r_study_member (
    id              BIGSERIAL       PRIMARY KEY,
    project_id      BIGINT          NOT NULL REFERENCES rdr.r_study_project(id),
    user_id         BIGINT          NOT NULL,
    role            VARCHAR(32)     NOT NULL,       -- PI/CO_INVESTIGATOR/RESEARCHER/DATA_MANAGER
    joined_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_project_member UNIQUE (project_id, user_id)
);

COMMENT ON TABLE rdr.r_study_member IS '项目成员';
```

#### 4.4.3 r_research_cohort - 研究队列

```sql
CREATE TABLE rdr.r_research_cohort (
    id              BIGSERIAL       PRIMARY KEY,
    project_id      BIGINT          NOT NULL REFERENCES rdr.r_study_project(id),
    cohort_name     VARCHAR(128)    NOT NULL,
    description     TEXT,
    inclusion_criteria JSONB,                       -- 纳入标准（结构化）
    exclusion_criteria JSONB,                       -- 排除标准（结构化）
    target_size     INT,
    current_size    INT             NOT NULL DEFAULT 0,
    status          VARCHAR(16)     NOT NULL DEFAULT 'DRAFT',
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_research_cohort IS '研究队列';
```

#### 4.4.4 r_study_subject - 研究受试者

```sql
CREATE TABLE rdr.r_study_subject (
    id              BIGSERIAL       PRIMARY KEY,
    cohort_id       BIGINT          NOT NULL REFERENCES rdr.r_research_cohort(id),
    patient_id      BIGINT          NOT NULL REFERENCES cdr.c_patient(id),
    subject_code    VARCHAR(32)     NOT NULL,       -- 受试者编号（脱敏）
    group_label     VARCHAR(32),                    -- 分组标签（实验组/对照组）
    enrolled_at     TIMESTAMP       NOT NULL DEFAULT NOW(),
    withdrawn_at    TIMESTAMP,
    withdrawal_reason TEXT,
    status          VARCHAR(16)     NOT NULL DEFAULT 'ENROLLED', -- ENROLLED/ACTIVE/WITHDRAWN/COMPLETED
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_subject_code UNIQUE (cohort_id, subject_code)
);

COMMENT ON TABLE rdr.r_study_subject IS '研究受试者';
CREATE INDEX idx_r_subject_cohort ON rdr.r_study_subject (cohort_id);
```

#### 4.4.5 r_dataset - 数据集

```sql
CREATE TABLE rdr.r_dataset (
    id              BIGSERIAL       PRIMARY KEY,
    project_id      BIGINT          NOT NULL REFERENCES rdr.r_study_project(id),
    dataset_name    VARCHAR(128)    NOT NULL,
    description     TEXT,
    dataset_type    VARCHAR(32)     NOT NULL,       -- STRUCTURED/IMAGING/GENOMIC/TEXT/MULTIMODAL
    source_type     VARCHAR(32)     NOT NULL,       -- CDR_EXTRACTION/UPLOAD/SYNTHETIC
    data_format     VARCHAR(32),                    -- CSV/PARQUET/DICOM/VCF/JSON
    record_count    BIGINT          NOT NULL DEFAULT 0,
    file_size_bytes BIGINT          NOT NULL DEFAULT 0,
    storage_path    VARCHAR(256),                   -- MinIO路径
    status          VARCHAR(16)     NOT NULL DEFAULT 'DRAFT', -- DRAFT/PUBLISHED/ARCHIVED
    access_level    VARCHAR(16)     NOT NULL DEFAULT 'PRIVATE', -- PRIVATE/PROJECT/PUBLIC
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_dataset IS '数据集';
CREATE INDEX idx_r_dataset_project ON rdr.r_dataset (project_id);
```

#### 4.4.6 r_dataset_version - 数据集版本

```sql
CREATE TABLE rdr.r_dataset_version (
    id              BIGSERIAL       PRIMARY KEY,
    dataset_id      BIGINT          NOT NULL REFERENCES rdr.r_dataset(id),
    version_no      VARCHAR(16)     NOT NULL,       -- 语义化版本 v1.0.0
    changelog       TEXT,
    record_count    BIGINT          NOT NULL DEFAULT 0,
    file_size_bytes BIGINT          NOT NULL DEFAULT 0,
    storage_path    VARCHAR(256),
    checksum        VARCHAR(64),                    -- SHA256校验
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(64)     NOT NULL,
    org_id          BIGINT          NOT NULL,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_dataset_version UNIQUE (dataset_id, version_no)
);

COMMENT ON TABLE rdr.r_dataset_version IS '数据集版本';
```

#### 4.4.7 r_dataset_access_log - 数据集访问日志

```sql
CREATE TABLE rdr.r_dataset_access_log (
    id              BIGSERIAL       PRIMARY KEY,
    dataset_id      BIGINT          NOT NULL REFERENCES rdr.r_dataset(id),
    user_id         BIGINT          NOT NULL,
    access_type     VARCHAR(16)     NOT NULL,       -- VIEW/DOWNLOAD/API_CALL
    purpose         TEXT,
    ip_address      VARCHAR(45),
    accessed_at     TIMESTAMP       NOT NULL DEFAULT NOW(),
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_dataset_access_log IS '数据集访问日志';
CREATE INDEX idx_r_access_dataset ON rdr.r_dataset_access_log (dataset_id);
CREATE INDEX idx_r_access_user ON rdr.r_dataset_access_log (user_id);
```

#### 4.4.8 r_clinical_feature - 临床特征

```sql
CREATE TABLE rdr.r_clinical_feature (
    id              BIGSERIAL       PRIMARY KEY,
    dataset_id      BIGINT          NOT NULL REFERENCES rdr.r_dataset(id),
    subject_id      BIGINT          REFERENCES rdr.r_study_subject(id),
    feature_code    VARCHAR(32)     NOT NULL,
    feature_name    VARCHAR(64)     NOT NULL,
    feature_value   TEXT,
    value_type      VARCHAR(16)     NOT NULL,       -- NUMERIC/CATEGORICAL/TEXT/DATE/BOOLEAN
    unit            VARCHAR(32),
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_clinical_feature IS '临床特征';
CREATE INDEX idx_r_feature_dataset ON rdr.r_clinical_feature (dataset_id);
```

#### 4.4.9 r_feature_dictionary - 特征字典

```sql
CREATE TABLE rdr.r_feature_dictionary (
    id              BIGSERIAL       PRIMARY KEY,
    feature_code    VARCHAR(32)     NOT NULL,
    feature_name    VARCHAR(64)     NOT NULL,
    category        VARCHAR(32),                    -- DEMOGRAPHIC/LAB/VITAL/IMAGING/GENOMIC
    data_type       VARCHAR(16)     NOT NULL,
    description     TEXT,
    reference_range VARCHAR(64),
    omop_concept_id INT,                            -- OMOP CDM映射
    loinc_code      VARCHAR(32),
    snomed_code     VARCHAR(32),
    is_standard     BOOLEAN         NOT NULL DEFAULT TRUE,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_feature_code UNIQUE (org_id, feature_code)
);

COMMENT ON TABLE rdr.r_feature_dictionary IS '特征字典';
```

#### 4.4.10 r_imaging_dataset - 影像数据集

```sql
CREATE TABLE rdr.r_imaging_dataset (
    id              BIGSERIAL       PRIMARY KEY,
    dataset_id      BIGINT          NOT NULL REFERENCES rdr.r_dataset(id),
    imaging_exam_id BIGINT          REFERENCES cdr.c_imaging_exam(id),
    subject_id      BIGINT          REFERENCES rdr.r_study_subject(id),
    modality        VARCHAR(16)     NOT NULL,       -- CT/MRI/XRAY/PT
    body_region     VARCHAR(32)     NOT NULL,
    file_format     VARCHAR(16)     NOT NULL DEFAULT 'DICOM', -- DICOM/NIfTI/NPZ
    file_path       VARCHAR(256)    NOT NULL,
    file_size_bytes BIGINT,
    pixel_spacing   VARCHAR(64),                    -- "0.5x0.5x1.0"
    image_size      VARCHAR(32),                    -- "512x512x128"
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_imaging_dataset IS '影像数据集';
CREATE INDEX idx_r_img_dataset ON rdr.r_imaging_dataset (dataset_id);
```

#### 4.4.11 r_imaging_annotation - 影像标注

```sql
CREATE TABLE rdr.r_imaging_annotation (
    id              BIGSERIAL       PRIMARY KEY,
    imaging_dataset_id BIGINT       NOT NULL REFERENCES rdr.r_imaging_dataset(id),
    annotator_id    BIGINT          NOT NULL,
    annotation_type VARCHAR(32)     NOT NULL,       -- BBOX/SEGMENTATION/LANDMARK/CLASSIFICATION
    label           VARCHAR(64)     NOT NULL,
    annotation_data JSONB           NOT NULL,       -- 标注数据（坐标/掩码等）
    annotation_format VARCHAR(16)   NOT NULL DEFAULT 'COCO', -- COCO/VOC/NIfTI/DICOM_SEG
    confidence      DECIMAL(3,2),                   -- 标注者置信度 0.00-1.00
    is_verified     BOOLEAN         DEFAULT FALSE,
    verified_by     BIGINT,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_imaging_annotation IS '影像标注';
CREATE INDEX idx_r_img_anno ON rdr.r_imaging_annotation (imaging_dataset_id);
```

#### 4.4.12 r_genomic_dataset - 基因组数据集

```sql
CREATE TABLE rdr.r_genomic_dataset (
    id              BIGSERIAL       PRIMARY KEY,
    dataset_id      BIGINT          NOT NULL REFERENCES rdr.r_dataset(id),
    subject_id      BIGINT          REFERENCES rdr.r_study_subject(id),
    seq_type        VARCHAR(16)     NOT NULL,       -- WGS/WES/RNA_SEQ/TARGETED
    platform        VARCHAR(32),                    -- ILLUMINA/ION_TORRENT/PACBIO
    file_format     VARCHAR(16)     NOT NULL DEFAULT 'VCF',
    file_path       VARCHAR(256)    NOT NULL,
    file_size_bytes BIGINT,
    reference_genome VARCHAR(32)    NOT NULL DEFAULT 'GRCh38',
    coverage        DECIMAL(6,1),                   -- 测序深度
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_genomic_dataset IS '基因组数据集';
CREATE INDEX idx_r_genomic_dataset ON rdr.r_genomic_dataset (dataset_id);
```

#### 4.4.13 r_genomic_variant - 基因变异

```sql
CREATE TABLE rdr.r_genomic_variant (
    id              BIGSERIAL       PRIMARY KEY,
    genomic_dataset_id BIGINT       NOT NULL REFERENCES rdr.r_genomic_dataset(id),
    chromosome      VARCHAR(4)      NOT NULL,
    position        INT             NOT NULL,
    ref_allele      VARCHAR(256)    NOT NULL,
    alt_allele      VARCHAR(256)    NOT NULL,
    variant_type    VARCHAR(16)     NOT NULL,       -- SNP/INDEL/CNV/SV
    gene_symbol     VARCHAR(32),
    transcript_id   VARCHAR(32),
    hgvs_c          VARCHAR(128),                   -- HGVS cDNA命名
    hgvs_p          VARCHAR(128),                   -- HGVS蛋白命名
    quality_score   DECIMAL(6,2),
    allele_freq     DECIMAL(8,6),
    clinical_significance VARCHAR(32),              -- PATHOGENIC/LIKELY_PATHOGENIC/VUS/LIKELY_BENIGN/BENIGN
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_genomic_variant IS '基因变异';
CREATE INDEX idx_r_variant_dataset ON rdr.r_genomic_variant (genomic_dataset_id);
CREATE INDEX idx_r_variant_gene ON rdr.r_genomic_variant (gene_symbol);
```

#### 4.4.14 r_text_dataset - 文本数据集

```sql
CREATE TABLE rdr.r_text_dataset (
    id              BIGSERIAL       PRIMARY KEY,
    dataset_id      BIGINT          NOT NULL REFERENCES rdr.r_dataset(id),
    subject_id      BIGINT          REFERENCES rdr.r_study_subject(id),
    source_type     VARCHAR(32)     NOT NULL,       -- CLINICAL_NOTE/PATHOLOGY_REPORT/IMAGING_REPORT
    source_id       BIGINT,                         -- 原始CDR记录ID
    text_content    TEXT            NOT NULL,
    language        VARCHAR(8)      NOT NULL DEFAULT 'zh',
    is_deidentified BOOLEAN         NOT NULL DEFAULT TRUE,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_text_dataset IS '文本数据集';
CREATE INDEX idx_r_text_dataset ON rdr.r_text_dataset (dataset_id);
```

#### 4.4.15 r_text_annotation - 文本标注

```sql
CREATE TABLE rdr.r_text_annotation (
    id              BIGSERIAL       PRIMARY KEY,
    text_dataset_id BIGINT          NOT NULL REFERENCES rdr.r_text_dataset(id),
    annotator_id    BIGINT          NOT NULL,
    annotation_type VARCHAR(32)     NOT NULL,       -- NER/RELATION/SENTIMENT/CLASSIFICATION
    entity_type     VARCHAR(32),                    -- DISEASE/SYMPTOM/DRUG/PROCEDURE
    start_offset    INT,                            -- 文本起始偏移
    end_offset      INT,                            -- 文本结束偏移
    entity_text     VARCHAR(256),                   -- 标注原文
    label           VARCHAR(64),
    attributes      JSONB,                          -- 扩展属性
    is_verified     BOOLEAN         DEFAULT FALSE,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_text_annotation IS '文本标注';
CREATE INDEX idx_r_text_anno ON rdr.r_text_annotation (text_dataset_id);
```

#### 4.4.16 r_etl_task - ETL任务

```sql
CREATE TABLE rdr.r_etl_task (
    id              BIGSERIAL       PRIMARY KEY,
    task_name       VARCHAR(128)    NOT NULL,
    project_id      BIGINT          REFERENCES rdr.r_study_project(id),
    source_type     VARCHAR(32)     NOT NULL,       -- CDR/FILE/EXTERNAL_API
    target_dataset_id BIGINT        REFERENCES rdr.r_dataset(id),
    etl_config      JSONB           NOT NULL,       -- ETL配置（映射规则、过滤条件等）
    schedule_type   VARCHAR(16)     NOT NULL DEFAULT 'MANUAL', -- MANUAL/SCHEDULED/REALTIME
    cron_expression VARCHAR(64),
    status          VARCHAR(16)     NOT NULL DEFAULT 'DRAFT', -- DRAFT/RUNNING/SUCCESS/FAILED/PAUSED
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    record_count    BIGINT          NOT NULL DEFAULT 0,
    error_count     INT             NOT NULL DEFAULT 0,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_etl_task IS 'ETL任务';
CREATE INDEX idx_r_etl_project ON rdr.r_etl_task (project_id);
```

#### 4.4.17 r_etl_task_log - ETL任务日志

```sql
CREATE TABLE rdr.r_etl_task_log (
    id              BIGSERIAL       PRIMARY KEY,
    task_id         BIGINT          NOT NULL REFERENCES rdr.r_etl_task(id),
    execution_no    INT             NOT NULL,       -- 第N次执行
    status          VARCHAR(16)     NOT NULL,       -- RUNNING/SUCCESS/FAILED
    started_at      TIMESTAMP       NOT NULL,
    finished_at     TIMESTAMP,
    records_read    BIGINT          DEFAULT 0,
    records_written BIGINT          DEFAULT 0,
    records_error   INT             DEFAULT 0,
    error_message   TEXT,
    execution_config JSONB,                         -- 本次执行的快照配置
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_etl_task_log IS 'ETL任务日志';
CREATE INDEX idx_r_etl_log_task ON rdr.r_etl_task_log (task_id);
```

#### 4.4.18 r_data_quality_rule - 数据质量规则

```sql
CREATE TABLE rdr.r_data_quality_rule (
    id              BIGSERIAL       PRIMARY KEY,
    rule_name       VARCHAR(128)    NOT NULL,
    rule_type       VARCHAR(32)     NOT NULL,       -- COMPLETENESS/ACCURACY/CONSISTENCY/TIMELINESS/UNIQUENESS
    target_table    VARCHAR(64)     NOT NULL,       -- 目标表名
    target_column   VARCHAR(64),                    -- 目标列名
    rule_expression TEXT            NOT NULL,       -- 规则表达式
    threshold       DECIMAL(5,2)    NOT NULL DEFAULT 100.00, -- 合格阈值(%)
    severity        VARCHAR(16)     NOT NULL DEFAULT 'WARNING', -- ERROR/WARNING/INFO
    is_enabled      BOOLEAN         NOT NULL DEFAULT TRUE,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_data_quality_rule IS '数据质量规则';
```

#### 4.4.19 r_data_quality_result - 数据质量检测结果

```sql
CREATE TABLE rdr.r_data_quality_result (
    id              BIGSERIAL       PRIMARY KEY,
    rule_id         BIGINT          NOT NULL REFERENCES rdr.r_data_quality_rule(id),
    dataset_id      BIGINT          REFERENCES rdr.r_dataset(id),
    etl_task_log_id BIGINT          REFERENCES rdr.r_etl_task_log(id),
    total_count     BIGINT          NOT NULL,
    pass_count      BIGINT          NOT NULL,
    fail_count      BIGINT          NOT NULL,
    pass_rate       DECIMAL(5,2)    NOT NULL,       -- 通过率(%)
    is_passed       BOOLEAN         NOT NULL,       -- 是否达标
    failed_samples  JSONB,                          -- 失败样本（最多100条）
    executed_at     TIMESTAMP       NOT NULL DEFAULT NOW(),
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE rdr.r_data_quality_result IS '数据质量检测结果';
CREATE INDEX idx_r_quality_rule ON rdr.r_data_quality_result (rule_id);
```

`★ Insight ─────────────────────────────────────`
**RDR设计要点**: 研究数据仓库以 `r_study_project` 为顶层入口，通过 `r_research_cohort` 定义研究队列，`r_dataset` 管理多模态数据集（结构化/影像/基因组/文本），`r_dataset_version` 实现版本化发布。ETL链路（`r_etl_task` + `r_etl_task_log`）记录从CDR到RDR的数据转换过程，`r_data_quality_rule` 提供数据质量保障。
`─────────────────────────────────────────────────`

---

### 4.5 Model - 模型管理

模型管理Schema支撑AI模型的全生命周期：注册→版本→评估→审批→部署→路由→监控。

#### 4.5.1 m_model - 模型注册

```sql
CREATE TABLE model.m_model (
    id              BIGSERIAL       PRIMARY KEY,
    model_code      VARCHAR(32)     NOT NULL,
    model_name      VARCHAR(128)    NOT NULL,
    description     TEXT,
    model_type      VARCHAR(32)     NOT NULL,       -- IMAGING/NLP/GENOMIC/STRUCTURED/MULTIMODAL
    task_type       VARCHAR(32)     NOT NULL,       -- CLASSIFICATION/SEGMENTATION/OBJECT_DETECTION/NAMED_ENTITY/REGRESSION
    framework       VARCHAR(32)     NOT NULL,       -- PYTORCH/TENSORFLOW/SKLEARN/XGBOOST/ONNX/OTHER
    input_schema    JSONB           NOT NULL,       -- 输入数据结构定义
    output_schema   JSONB           NOT NULL,       -- 输出数据结构定义
    tags            VARCHAR(256),                   -- 标签（逗号分隔）
    license         VARCHAR(32),
    owner_id        BIGINT          NOT NULL,
    project_id      BIGINT          REFERENCES rdr.r_study_project(id),
    status          VARCHAR(16)     NOT NULL DEFAULT 'DRAFT', -- DRAFT/REGISTERED/PUBLISHED/DEPRECATED
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_model_code UNIQUE (org_id, model_code)
);

COMMENT ON TABLE model.m_model IS '模型注册';
```

#### 4.5.2 m_model_version - 模型版本

```sql
CREATE TABLE model.m_model_version (
    id              BIGSERIAL       PRIMARY KEY,
    model_id        BIGINT          NOT NULL REFERENCES model.m_model(id),
    version_no      VARCHAR(16)     NOT NULL,       -- 语义化版本 v1.0.0
    description     TEXT,
    changelog       TEXT,
    framework_version VARCHAR(32),
    model_file_path VARCHAR(256)    NOT NULL,       -- MinIO存储路径
    model_file_size BIGINT,
    model_file_checksum VARCHAR(64),                -- SHA256
    config_path     VARCHAR(256),                   -- 配置文件路径
    hyper_params    JSONB,                          -- 超参数
    training_dataset_id BIGINT       REFERENCES rdr.r_dataset(id),
    training_metrics JSONB,                         -- 训练指标 {loss, accuracy, epochs...}
    status          VARCHAR(16)     NOT NULL DEFAULT 'CREATED', -- CREATED/TRAINING/EVALUATING/APPROVED/DEPLOYED/DEPRECATED
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_model_version UNIQUE (model_id, version_no)
);

COMMENT ON TABLE model.m_model_version IS '模型版本';
CREATE INDEX idx_m_version_model ON model.m_model_version (model_id);
```

#### 4.5.3 m_evaluation - 模型评估

```sql
CREATE TABLE model.m_evaluation (
    id              BIGSERIAL       PRIMARY KEY,
    model_version_id BIGINT         NOT NULL REFERENCES model.m_model_version(id),
    eval_name       VARCHAR(128)    NOT NULL,
    eval_type       VARCHAR(32)     NOT NULL,       -- INTERNAL/EXTERNAL/CROSS_VALIDATION
    dataset_id      BIGINT          NOT NULL REFERENCES rdr.r_dataset(id),
    metrics         JSONB           NOT NULL,       -- 评估指标 {auc, f1, precision, recall, iou...}
    confusion_matrix JSONB,                         -- 混淆矩阵
    roc_data        JSONB,                          -- ROC曲线数据点
    error_analysis  JSONB,                          -- 错误分析
    eval_report_path VARCHAR(256),                  -- 评估报告存储路径
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING', -- PENDING/RUNNING/COMPLETED/FAILED
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE model.m_evaluation IS '模型评估';
CREATE INDEX idx_m_eval_version ON model.m_evaluation (model_version_id);
```

#### 4.5.4 m_approval - 模型审批

```sql
CREATE TABLE model.m_approval (
    id              BIGSERIAL       PRIMARY KEY,
    model_version_id BIGINT         NOT NULL REFERENCES model.m_model_version(id),
    approval_type   VARCHAR(32)     NOT NULL,       -- DEPLOY/PUBLISH/CLINICAL_USE
    applicant_id    BIGINT          NOT NULL,
    reviewer_id     BIGINT,
    result          VARCHAR(16),                    -- PENDING/APPROVED/REJECTED
    result_comment  TEXT,
    submitted_at    TIMESTAMP       NOT NULL DEFAULT NOW(),
    reviewed_at     TIMESTAMP,
    -- 评审材料
    evidence_docs   JSONB,                          -- [{name, path, type}]
    risk_assessment TEXT,                           -- 风险评估
    clinical_validation TEXT,                       -- 临床验证说明
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE model.m_approval IS '模型审批';
CREATE INDEX idx_m_approval_version ON model.m_approval (model_version_id);
```

#### 4.5.5 m_deployment - 模型部署

```sql
CREATE TABLE model.m_deployment (
    id              BIGSERIAL       PRIMARY KEY,
    model_version_id BIGINT         NOT NULL REFERENCES model.m_model_version(id),
    deployment_name VARCHAR(128)    NOT NULL,
    deployment_type VARCHAR(32)     NOT NULL,       -- ONLINE/BATCH/EDGE
    serving_framework VARCHAR(32)   NOT NULL,       -- TRITON/TORCHSERVE/TFSERVING/ONNX_RUNTIME/FASTAPI
    endpoint_url    VARCHAR(256),                   -- 推理API端点
    resource_config JSONB           NOT NULL,       -- {cpu, memory, gpu, replicas}
    env_vars        JSONB,                          -- 环境变量
    auto_scale      BOOLEAN         NOT NULL DEFAULT FALSE,
    min_replicas    INT             NOT NULL DEFAULT 1,
    max_replicas    INT             NOT NULL DEFAULT 3,
    health_check_url VARCHAR(256),
    status          VARCHAR(16)     NOT NULL DEFAULT 'CREATING', -- CREATING/RUNNING/STOPPING/STOPPED/FAILED
    deployed_at     TIMESTAMP,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE model.m_deployment IS '模型部署';
CREATE INDEX idx_m_deploy_version ON model.m_deployment (model_version_id);
```

#### 4.5.6 m_deploy_route - 部署路由

```sql
CREATE TABLE model.m_deploy_route (
    id              BIGSERIAL       PRIMARY KEY,
    route_name      VARCHAR(128)    NOT NULL,
    model_id        BIGINT          NOT NULL REFERENCES model.m_model(id),
    route_type      VARCHAR(32)     NOT NULL,       -- CANARY/AB_TEST/WEIGHTED/MIRROR
    config          JSONB           NOT NULL,       -- 路由配置 [{deployment_id, weight}]
    default_deployment_id BIGINT    NOT NULL REFERENCES model.m_deployment(id),
    traffic_rules   JSONB,                          -- 流量分配规则
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE model.m_deploy_route IS '部署路由';
CREATE INDEX idx_m_route_model ON model.m_deploy_route (model_id);
```

#### 4.5.7 m_inference_log - 推理日志（按时间分区）

```sql
CREATE TABLE model.m_inference_log (
    id              BIGSERIAL,
    deployment_id   BIGINT          NOT NULL,
    request_id      VARCHAR(64)     NOT NULL,
    patient_id      BIGINT,
    encounter_id    BIGINT,
    input_summary   JSONB,                          -- 输入摘要（不含原始数据）
    output_result   JSONB           NOT NULL,       -- 推理结果
    confidence      DECIMAL(5,4),                   -- 置信度 0.0000-1.0000
    latency_ms      INT,                            -- 推理耗时(ms)
    model_version_no VARCHAR(16)    NOT NULL,
    caller_service  VARCHAR(64),                    -- 调用方服务标识
    caller_user_id  BIGINT,
    status          VARCHAR(16)     NOT NULL DEFAULT 'SUCCESS', -- SUCCESS/ERROR/TIMEOUT
    error_message   TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    org_id          BIGINT          NOT NULL
) PARTITION BY RANGE (created_at);

-- 按月创建分区（示例）
CREATE TABLE model.m_inference_log_2026_01 PARTITION OF model.m_inference_log
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE model.m_inference_log_2026_02 PARTITION OF model.m_inference_log
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE model.m_inference_log_2026_03 PARTITION OF model.m_inference_log
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE model.m_inference_log_2026_04 PARTITION OF model.m_inference_log
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE model.m_inference_log_default PARTITION OF model.m_inference_log
    DEFAULT;

-- 分区表的主键和索引需单独创建
CREATE UNIQUE INDEX uk_inference_log_id ON model.m_inference_log (id, created_at);
CREATE INDEX idx_inference_deploy ON model.m_inference_log (deployment_id, created_at);
CREATE INDEX idx_inference_request ON model.m_inference_log (request_id);

COMMENT ON TABLE model.m_inference_log IS '推理日志（按月分区）';
```

#### 4.5.8 m_model_metric - 模型运行指标

```sql
CREATE TABLE model.m_model_metric (
    id              BIGSERIAL       PRIMARY KEY,
    deployment_id   BIGINT          NOT NULL REFERENCES model.m_deployment(id),
    metric_time     TIMESTAMP       NOT NULL,       -- 指标采集时间
    metric_type     VARCHAR(32)     NOT NULL,       -- PERFORMANCE/ACCURACY/RESOURCE
    metric_name     VARCHAR(64)     NOT NULL,       -- qps/latency_p50/latency_p99/gpu_util/accuracy
    metric_value    DECIMAL(16,4)   NOT NULL,
    tags            JSONB,                          -- 标签维度 {model_version, dataset}
    -- 公共字段
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE model.m_model_metric IS '模型运行指标';
CREATE INDEX idx_m_metric_deploy ON model.m_model_metric (deployment_id, metric_time);
CREATE INDEX idx_m_metric_name ON model.m_model_metric (metric_name, metric_time);
```

#### 4.5.9 m_alert_rule - 告警规则

```sql
CREATE TABLE model.m_alert_rule (
    id              BIGSERIAL       PRIMARY KEY,
    rule_name       VARCHAR(128)    NOT NULL,
    deployment_id   BIGINT          REFERENCES model.m_deployment(id),
    metric_name     VARCHAR(64)     NOT NULL,
    condition       VARCHAR(16)     NOT NULL,       -- GT/GTE/LT/LTE/EQ/NE
    threshold       DECIMAL(16,4)   NOT NULL,
    duration_sec    INT,                            -- 持续时间（秒）
    severity        VARCHAR(16)     NOT NULL DEFAULT 'WARNING', -- CRITICAL/WARNING/INFO
    notify_channels JSONB           NOT NULL,       -- ["email","sms","webhook"]
    notify_users    JSONB,                          -- [user_id列表]
    is_enabled      BOOLEAN         NOT NULL DEFAULT TRUE,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE model.m_alert_rule IS '告警规则';
```

#### 4.5.10 m_alert_record - 告警记录

```sql
CREATE TABLE model.m_alert_record (
    id              BIGSERIAL       PRIMARY KEY,
    rule_id         BIGINT          NOT NULL REFERENCES model.m_alert_rule(id),
    deployment_id   BIGINT          NOT NULL,
    alert_time      TIMESTAMP       NOT NULL,
    metric_value    DECIMAL(16,4)   NOT NULL,
    threshold       DECIMAL(16,4)   NOT NULL,
    message         TEXT            NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'FIRING', -- FIRING/ACKNOWLEDGED/RESOLVED
    acknowledged_by BIGINT,
    acknowledged_at TIMESTAMP,
    resolved_at     TIMESTAMP,
    -- 公共字段
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE model.m_alert_record IS '告警记录';
CREATE INDEX idx_m_alert_rule ON model.m_alert_record (rule_id);
CREATE INDEX idx_m_alert_time ON model.m_alert_record (alert_time);
```

`★ Insight ─────────────────────────────────────`
**推理日志分区表**: `m_inference_log` 使用PostgreSQL声明式范围分区（PARTITION BY RANGE），按月自动分割。这在高并发推理场景下至关重要——单月可能产生数百万条推理记录，分区后查询和过期清理都只需操作单个分区。注意分区表的主键必须包含分区键（`created_at`）。
`─────────────────────────────────────────────────`

---

### 4.6 System - 系统管理

#### 4.6.1 s_user - 用户

```sql
CREATE TABLE system.s_user (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(64)     NOT NULL,
    password_hash   VARCHAR(256)    NOT NULL,
    real_name       VARCHAR(64)     NOT NULL,
    email           VARCHAR(128),
    phone           VARCHAR(32),
    avatar_url      VARCHAR(256),
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/DISABLED/LOCKED
    last_login_at   TIMESTAMP,
    last_login_ip   VARCHAR(45),
    password_changed_at TIMESTAMP,
    must_change_pwd BOOLEAN         NOT NULL DEFAULT FALSE,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_username UNIQUE (org_id, username)
);

COMMENT ON TABLE system.s_user IS '用户';
```

#### 4.6.2 s_role - 角色

```sql
CREATE TABLE system.s_role (
    id              BIGSERIAL       PRIMARY KEY,
    role_code       VARCHAR(32)     NOT NULL,
    role_name       VARCHAR(64)     NOT NULL,
    description     TEXT,
    is_system       BOOLEAN         NOT NULL DEFAULT FALSE, -- 系统内置角色不可删除
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_role_code UNIQUE (org_id, role_code)
);

COMMENT ON TABLE system.s_role IS '角色';
```

#### 4.6.3 s_permission - 权限

```sql
CREATE TABLE system.s_permission (
    id              BIGSERIAL       PRIMARY KEY,
    permission_code VARCHAR(64)     NOT NULL,
    permission_name VARCHAR(128)    NOT NULL,
    resource_type   VARCHAR(32)     NOT NULL,       -- MENU/BUTTON/API/DATA
    resource_key    VARCHAR(128)    NOT NULL,       -- 资源标识
    action          VARCHAR(32)     NOT NULL,       -- CREATE/READ/UPDATE/DELETE/EXECUTE
    parent_id       BIGINT          REFERENCES system.s_permission(id),
    sort_order      INT             NOT NULL DEFAULT 0,
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_perm_code UNIQUE (org_id, permission_code)
);

COMMENT ON TABLE system.s_permission IS '权限';
```

#### 4.6.4 s_user_role - 用户角色关联

```sql
CREATE TABLE system.s_user_role (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES system.s_user(id),
    role_id         BIGINT          NOT NULL REFERENCES system.s_role(id),
    granted_by      BIGINT          NOT NULL,
    granted_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

COMMENT ON TABLE system.s_user_role IS '用户角色关联';
```

#### 4.6.5 s_role_permission - 角色权限关联

```sql
CREATE TABLE system.s_role_permission (
    id              BIGSERIAL       PRIMARY KEY,
    role_id         BIGINT          NOT NULL REFERENCES system.s_role(id),
    permission_id   BIGINT          NOT NULL REFERENCES system.s_permission(id),
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_role_perm UNIQUE (role_id, permission_id)
);

COMMENT ON TABLE system.s_role_permission IS '角色权限关联';
```

#### 4.6.6 s_dict - 数据字典

```sql
CREATE TABLE system.s_dict (
    id              BIGSERIAL       PRIMARY KEY,
    dict_type       VARCHAR(32)     NOT NULL,
    dict_code       VARCHAR(32)     NOT NULL,
    dict_label      VARCHAR(128)    NOT NULL,
    dict_value      VARCHAR(256),
    sort_order      INT             NOT NULL DEFAULT 0,
    parent_code     VARCHAR(32),
    is_enabled      BOOLEAN         NOT NULL DEFAULT TRUE,
    remark          VARCHAR(256),
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_dict UNIQUE (org_id, dict_type, dict_code)
);

COMMENT ON TABLE system.s_dict IS '数据字典';
```

#### 4.6.7 s_config - 系统配置

```sql
CREATE TABLE system.s_config (
    id              BIGSERIAL       PRIMARY KEY,
    config_key      VARCHAR(128)    NOT NULL,
    config_value    TEXT            NOT NULL,
    config_type     VARCHAR(16)     NOT NULL DEFAULT 'STRING', -- STRING/NUMBER/BOOLEAN/JSON
    description     VARCHAR(256),
    is_encrypted    BOOLEAN         NOT NULL DEFAULT FALSE, -- 值是否加密存储
    -- 公共字段
    created_by      VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL,
    CONSTRAINT uk_config_key UNIQUE (org_id, config_key)
);

COMMENT ON TABLE system.s_config IS '系统配置';
```

---

### 4.7 Audit - 审计日志

#### 4.7.1 a_audit_log - 操作审计日志

```sql
CREATE TABLE audit.a_audit_log (
    id              BIGSERIAL       PRIMARY KEY,
    trace_id        VARCHAR(64)     NOT NULL,       -- 链路追踪ID
    user_id         BIGINT,
    username        VARCHAR(64),
    service_name    VARCHAR(64)     NOT NULL,       -- 来源服务
    operation       VARCHAR(32)     NOT NULL,       -- CREATE/READ/UPDATE/DELETE/LOGIN/LOGOUT
    resource_type   VARCHAR(32)     NOT NULL,       -- 资源类型
    resource_id     VARCHAR(64),                    -- 资源ID
    resource_name   VARCHAR(128),                   -- 资源名称
    request_method  VARCHAR(8),                     -- HTTP方法
    request_url     VARCHAR(256),
    request_params  JSONB,                          -- 请求参数（脱敏）
    response_code   INT,
    response_msg    VARCHAR(256),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(256),
    duration_ms     INT,                            -- 执行耗时
    status          VARCHAR(16)     NOT NULL,       -- SUCCESS/FAILURE
    error_message   TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE audit.a_audit_log IS '操作审计日志';
CREATE INDEX idx_audit_trace ON audit.a_audit_log (trace_id);
CREATE INDEX idx_audit_user ON audit.a_audit_log (user_id, created_at);
CREATE INDEX idx_audit_resource ON audit.a_audit_log (resource_type, resource_id);
CREATE INDEX idx_audit_time ON audit.a_audit_log (created_at);
```

#### 4.7.2 a_data_access_log - 数据访问审计

```sql
CREATE TABLE audit.a_data_access_log (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    access_type     VARCHAR(32)     NOT NULL,       -- QUERY/EXPORT/DOWNLOAD/SHARE
    data_domain     VARCHAR(32)     NOT NULL,       -- CDR/RDR/MODEL
    table_name      VARCHAR(64),
    record_id       BIGINT,
    patient_id      BIGINT,
    purpose         TEXT,
    data_volume     BIGINT,                         -- 涉及数据量（条数）
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE audit.a_data_access_log IS '数据访问审计';
CREATE INDEX idx_dal_user ON audit.a_data_access_log (user_id, created_at);
CREATE INDEX idx_dal_patient ON audit.a_data_access_log (patient_id);
```

#### 4.7.3 a_system_event - 系统事件日志

```sql
CREATE TABLE audit.a_system_event (
    id              BIGSERIAL       PRIMARY KEY,
    event_type      VARCHAR(32)     NOT NULL,       -- SERVICE_START/SERVICE_STOP/CONFIG_CHANGE/DEPLOY/ALERT
    event_level     VARCHAR(16)     NOT NULL,       -- INFO/WARN/ERROR/CRITICAL
    source          VARCHAR(64)     NOT NULL,       -- 来源服务/组件
    event_title     VARCHAR(128)    NOT NULL,
    event_detail    TEXT,
    event_data      JSONB,
    resolved        BOOLEAN         DEFAULT FALSE,
    resolved_by     VARCHAR(64),
    resolved_at     TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    org_id          BIGINT          NOT NULL
);

COMMENT ON TABLE audit.a_system_event IS '系统事件日志';
CREATE INDEX idx_sys_event_type ON audit.a_system_event (event_type, created_at);
CREATE INDEX idx_sys_event_level ON audit.a_system_event (event_level, created_at);
```

---

## 5. 微服务设计

### 5.1 服务总览

MAIDC由8个核心微服务组成，通过Nacos注册发现，Spring Cloud Gateway统一路由。

```
                    ┌──────────────────────┐
                    │   Spring Cloud       │
                    │   Gateway            │
                    └──────────┬───────────┘
                               │
    ┌──────────┬───────────┬───┴───┬──────────┬──────────┬──────────┬──────────┐
    │          │           │       │          │          │          │          │
┌───▼───┐ ┌───▼───┐ ┌─────▼──┐ ┌──▼───┐ ┌───▼───┐ ┌───▼───┐ ┌───▼───┐ ┌───▼──────┐
│ maidc  │ │ maidc  │ │ maidc  │ │maidc │ │ maidc │ │ maidc │ │ maidc │ │ maidc-   │
│ -auth  │ │ -data │ │ -model │ │-task │ │-label │ │ -audit│ │ -msg  │ │ aiworker │
│        │ │        │ │  ★P1   │ │      │ │       │ │       │ │       │ │          │
└────────┘ └────────┘ └────────┘ └──────┘ └───────┘ └───────┘ └───────┘ └──────────┘
```

### 5.2 服务详细设计

#### 5.2.1 maidc-auth - 用户权限服务

| 属性 | 说明 |
|------|------|
| **技术栈** | Java 17 / Spring Boot 3.x / Spring Security |
| **端口** | 8081 |
| **数据库** | system schema |
| **核心职责** | 用户认证、权限管理、角色分配、菜单管理 |

**功能模块**:

1. **认证管理**
   - JWT令牌签发与刷新
   - 密码策略（复杂度、过期、锁定）
   - 多因子认证预留（SMS/OTP）
   - 单点登录（SSO）预留

2. **权限管理（RBAC）**
   - 用户-角色-权限三级模型
   - 菜单权限 + API权限 + 数据权限
   - 数据权限按 `org_id` 隔离
   - 权限缓存（Redis）

3. **组织架构**
   - 多机构管理（`c_org`）
   - 科室/部门管理
   - 用户与机构绑定

**关键接口**:

```
POST   /api/auth/login              # 登录
POST   /api/auth/refresh             # 刷新令牌
POST   /api/auth/logout              # 登出
GET    /api/users                    # 用户列表
POST   /api/users                    # 创建用户
PUT    /api/users/{id}               # 更新用户
GET    /api/roles                    # 角色列表
POST   /api/roles                    # 创建角色
PUT    /api/roles/{id}/permissions   # 分配权限
GET    /api/permissions/tree         # 权限树
```

---

#### 5.2.2 maidc-data - 数据管理服务

| 属性 | 说明 |
|------|------|
| **技术栈** | Java 17 / Spring Boot 3.x |
| **端口** | 8082 |
| **数据库** | cdr + rdr schema |
| **核心职责** | CDR数据接入、RDR数据集管理、ETL调度、数据质量 |

**功能模块**:

1. **CDR数据管理**
   - 患者信息CRUD（支持脱敏查询）
   - 就诊/诊断/检验/影像等临床数据管理
   - 数据源连接管理（HIS/PACS/LIS对接）
   - 数据字典映射（ICD-10、LOINC、SNOMED CT）

2. **RDR数据集管理**
   - 科研项目管理
   - 研究队列定义与受试者管理
   - 数据集创建与版本发布
   - 特征工程（特征字典 + 特征提取）

3. **ETL引擎**
   - ETL任务配置与管理
   - 支持定时/手动/实时触发
   - ETL执行日志与错误追踪
   - 数据质量规则配置与检测

**依赖服务**: maidc-auth（权限校验）

**关键接口**:

```
# CDR
GET    /api/cdr/patients             # 患者列表（脱敏）
GET    /api/cdr/patients/{id}        # 患者详情
GET    /api/cdr/encounters           # 就诊列表
GET    /api/cdr/encounters/{id}      # 就诊详情（含关联数据）

# RDR
POST   /api/rdr/projects             # 创建研究项目
GET    /api/rdr/projects/{id}/cohorts # 队列列表
POST   /api/rdr/datasets             # 创建数据集
POST   /api/rdr/datasets/{id}/versions # 发布版本

# ETL
POST   /api/etl/tasks                # 创建ETL任务
POST   /api/etl/tasks/{id}/execute   # 执行ETL
GET    /api/etl/tasks/{id}/logs       # 执行日志

# 数据质量
POST   /api/quality/rules            # 创建质量规则
GET    /api/quality/results           # 质量检测结果
```

---

#### 5.2.3 maidc-model - 模型管理服务 ★Phase 1

| 属性 | 说明 |
|------|------|
| **技术栈** | Java 17 / Spring Boot 3.x |
| **端口** | 8083 |
| **数据库** | model schema |
| **核心职责** | 模型注册、版本管理、评估、审批、部署、监控 |
| **优先级** | **Phase 1 优先实现** |

**功能模块**:

1. **模型注册与管理**
   - 模型信息注册（名称、类型、框架、输入输出Schema）
   - 模型标签与分类管理
   - 模型生命周期状态机（DRAFT→REGISTERED→PUBLISHED→DEPRECATED）

2. **版本管理**
   - 语义化版本号（v主.次.补丁）
   - 模型文件上传（MinIO存储）
   - 超参数、训练指标记录
   - 版本对比

3. **模型评估**
   - 评估任务创建与执行
   - 支持内部评估、外部验证、交叉验证
   - 评估指标计算与可视化
   - 评估报告生成

4. **审批流程**
   - 多级审批（技术评审→临床评审→管理审批）
   - 审批材料管理
   - 审批历史追踪

5. **部署管理**
   - 在线推理/批量推理/边缘部署
   - 多框架推理服务（Triton/TorchServe/ONNX Runtime）
   - 资源配置（CPU/GPU/内存）
   - 自动扩缩容

6. **流量路由**
   - 金丝雀发布（Canary）
   - A/B测试
   - 加权流量分配
   - 流量镜像

7. **监控告警**
   - 推理日志采集（分区存储）
   - 运行指标采集（QPS、延迟、GPU利用率）
   - 自定义告警规则
   - 告警通知（邮件/短信/Webhook）

**依赖服务**: maidc-auth, maidc-data（关联数据集）, maidc-aiworker（异步评估/推理）

**状态机**:

```
模型注册:  DRAFT → REGISTERED → PUBLISHED → DEPRECATED
版本:      CREATED → TRAINING → EVALUATING → APPROVED → DEPLOYED → DEPRECATED
部署:      CREATING → RUNNING → STOPPING → STOPPED / FAILED
审批:      PENDING → APPROVED / REJECTED
告警:      FIRING → ACKNOWLEDGED → RESOLVED
```

**关键接口**:

```
# 模型管理
POST   /api/models                    # 注册模型
GET    /api/models                    # 模型列表
GET    /api/models/{id}               # 模型详情
PUT    /api/models/{id}               # 更新模型
DELETE /api/models/{id}               # 删除模型

# 版本管理
POST   /api/models/{id}/versions      # 创建版本
GET    /api/models/{id}/versions       # 版本列表
GET    /api/models/{id}/versions/{vid} # 版本详情
PUT    /api/models/{id}/versions/{vid} # 更新版本

# 评估
POST   /api/evaluations               # 创建评估
GET    /api/evaluations/{id}           # 评估详情
GET    /api/evaluations/{id}/report    # 评估报告

# 审批
POST   /api/approvals                  # 提交审批
PUT    /api/approvals/{id}/review      # 审批操作

# 部署
POST   /api/deployments                # 创建部署
GET    /api/deployments                # 部署列表
PUT    /api/deployments/{id}/start     # 启动部署
PUT    /api/deployments/{id}/stop      # 停止部署
GET    /api/deployments/{id}/status    # 部署状态

# 路由
POST   /api/routes                     # 创建路由
PUT    /api/routes/{id}                # 更新路由

# 监控
GET    /api/monitoring/deployments/{id}/metrics  # 指标查询
GET    /api/monitoring/deployments/{id}/logs     # 推理日志
POST   /api/alert-rules                # 创建告警规则
GET    /api/alerts                     # 告警列表
PUT    /api/alerts/{id}/acknowledge    # 确认告警
```

---

#### 5.2.4 maidc-task - 任务调度服务

| 属性 | 说明 |
|------|------|
| **技术栈** | Java 17 / Spring Boot 3.x / XXL-JOB |
| **端口** | 8084 |
| **数据库** | 独立XXL-JOB库 + 业务表 |
| **核心职责** | 分布式定时任务调度、异步任务管理 |

**功能模块**:

1. **定时任务管理**
   - 基于XXL-JOB的分布式调度
   - Cron表达式配置
   - 任务分组与路由策略
   - 失败重试与告警

2. **异步任务管理**
   - 长时任务状态追踪
   - 进度上报
   - 取消/重试
   - 任务优先级队列

3. **典型调度场景**
   - CDR数据同步（每日）
   - ETL定时执行
   - 数据质量检测（每日）
   - 模型性能指标聚合（每5分钟）
   - 过期数据清理（每周）

---

#### 5.2.5 maidc-label - 数据标注服务

| 属性 | 说明 |
|------|------|
| **技术栈** | Java 17 / Spring Boot 3.x |
| **端口** | 8085 |
| **数据库** | rdr schema（标注相关表） |
| **核心职责** | 多模态数据标注、标注质量管理 |

**功能模块**:

1. **标注任务管理**
   - 标注任务创建与分配
   - 支持影像/文本/基因组标注
   - 多人协同标注
   - 标注进度追踪

2. **标注工具集成**
   - 影像标注：矩形框/多边形/关键点/分割掩码
   - 文本标注：实体标注/关系标注/分类
   - 标注格式：COCO/VOC/NIfTI/DICOM-Seg

3. **质量控制**
   - 多人交叉标注
   - 一致性检验（Cohen's Kappa / IoU）
   - 专家审核
   - AI辅助预标注

---

#### 5.2.6 maidc-audit - 审计日志服务

| 属性 | 说明 |
|------|------|
| **技术栈** | Java 17 / Spring Boot 3.x |
| **端口** | 8086 |
| **数据库** | audit schema |
| **核心职责** | 操作审计、数据访问追踪、系统事件记录 |

**功能模块**:

1. **操作审计**
   - 全链路操作日志（通过Gateway Filter + AOP采集）
   - 链路追踪ID串联
   - 请求/响应摘要（自动脱敏）

2. **数据访问审计**
   - 患者数据访问记录
   - 数据导出/下载追踪
   - 科研数据使用审计

3. **合规报表**
   - 数据访问统计报表
   - 用户操作审计报表
   - 异常行为检测

---

#### 5.2.7 maidc-msg - 消息通知服务

| 属性 | 说明 |
|------|------|
| **技术栈** | Java 17 / Spring Boot 3.x |
| **端口** | 8087 |
| **数据库** | 独立消息表 |
| **核心职责** | 站内消息、邮件、短信、Webhook通知 |

**功能模块**:

1. **通知渠道**
   - 站内消息（WebSocket实时推送）
   - 邮件（SMTP）
   - 短信（阿里云SMS）
   - Webhook（企业微信/钉钉）

2. **通知场景**
   - 审批通知（待审批/已审批）
   - 告警通知（模型异常/系统告警）
   - 任务通知（ETL完成/评估完成）
   - 系统通知（版本更新/维护公告）

3. **消息管理**
   - 已读/未读管理
   - 消息模板
   - 通知偏好设置
   - 消息归档

---

#### 5.2.8 maidc-aiworker - Python AI Worker集群

| 属性 | 说明 |
|------|------|
| **技术栈** | Python 3.10+ / FastAPI / Celery / RabbitMQ |
| **端口** | 8090（FastAPI） |
| **数据库** | 只读model/rdr schema |
| **核心职责** | 模型推理执行、模型评估、AI预处理 |

**功能模块**:

1. **推理服务（FastAPI）**
   - 同步推理API（低延迟场景）
   - 批量推理API（高吞吐场景）
   - 多框架推理引擎（PyTorch/ONNX/TensorRT）
   - GPU资源管理

2. **异步任务（Celery Worker）**
   - 模型评估执行
   - 数据预处理（影像预处理/NLP分词/基因变异标注）
   - AI辅助预标注
   - 推理结果后处理

3. **Worker管理**
   - 多Worker节点注册
   - GPU资源调度
   - 任务队列管理（按优先级）
   - Worker健康检查

**架构图**:

```
┌──────────────┐     RabbitMQ      ┌─────────────────────┐
│ maidc-model  │──────────────────▶│  Celery Worker 1    │ (GPU 0)
│ (Java)       │                   │  Celery Worker 2    │ (GPU 1)
└──────┬───────┘                   │  Celery Worker N    │ (CPU)
       │                           └─────────────────────┘
       │ HTTP
       ▼
┌──────────────┐
│ FastAPI       │ ←── 同步推理请求（低延迟）
│ 推理服务      │
└──────────────┘
```

**Celery队列设计**:

| 队列名 | 用途 | 并发 |
|--------|------|------|
| `inference` | 实时推理任务 | 高优先级 |
| `evaluation` | 模型评估 | 中优先级 |
| `preprocessing` | 数据预处理 | 低优先级 |
| `batch_inference` | 批量推理 | 低优先级 |

---

## 6. API设计

### 6.1 API通用规范

#### 6.1.1 URL规范

- 基础路径: `/api/{version}/{resource}`
- 版本控制: URL路径版本 `/api/v1/`
- 资源命名: 小写复数名词 `/api/v1/models`
- 关联资源: `/api/v1/models/{id}/versions`

#### 6.1.2 统一响应格式

```json
{
    "code": 200,
    "message": "success",
    "data": { },
    "trace_id": "a1b2c3d4-e5f6-7890"
}
```

#### 6.1.3 分页格式

```json
{
    "code": 200,
    "data": {
        "items": [],
        "total": 1000,
        "page": 1,
        "page_size": 20,
        "total_pages": 50
    }
}
```

#### 6.1.4 错误码定义

| 错误码 | 含义 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如版本号重复） |
| 422 | 数据验证失败 |
| 429 | 请求限流 |
| 500 | 服务内部错误 |
| 503 | 服务不可用 |

---

### 6.2 模型管理API（Phase 1核心）

#### 6.2.1 模型注册与管理

**注册模型**

```
POST /api/v1/models
Content-Type: application/json

请求体:
{
    "model_code": "LUNG_NODULE_DET_001",
    "model_name": "肺结节检测模型",
    "description": "基于3D CNN的肺结节检测模型，支持CT影像",
    "model_type": "IMAGING",
    "task_type": "OBJECT_DETECTION",
    "framework": "PYTORCH",
    "input_schema": {
        "type": "object",
        "properties": {
            "image": { "type": "dicom_series", "description": "CT影像序列" },
            "slice_thickness": { "type": "number", "description": "层厚(mm)" }
        },
        "required": ["image"]
    },
    "output_schema": {
        "type": "array",
        "items": {
            "type": "object",
            "properties": {
                "bbox": { "type": "array", "items": "number", "description": "[x,y,w,h,d]" },
                "confidence": { "type": "number" },
                "label": { "type": "string", "enum": ["nodule"] },
                "classification": { "type": "string", "enum": ["benign","suspicious","malignant"] }
            }
        }
    },
    "tags": ["肺部", "CT", "结节检测", "3D-CNN"],
    "license": "MIT",
    "project_id": 1
}

响应: 201 Created
{
    "code": 201,
    "data": {
        "id": 1,
        "model_code": "LUNG_NODULE_DET_001",
        "status": "DRAFT",
        "created_at": "2026-04-08T10:00:00Z"
    }
}
```

**查询模型列表**

```
GET /api/v1/models?page=1&page_size=20&model_type=IMAGING&status=PUBLISHED&keyword=肺结节

响应: 200 OK
{
    "code": 200,
    "data": {
        "items": [
            {
                "id": 1,
                "model_code": "LUNG_NODULE_DET_001",
                "model_name": "肺结节检测模型",
                "model_type": "IMAGING",
                "task_type": "OBJECT_DETECTION",
                "framework": "PYTORCH",
                "status": "PUBLISHED",
                "latest_version": "v1.2.0",
                "tags": ["肺部", "CT"],
                "owner_name": "张三",
                "created_at": "2026-04-08T10:00:00Z",
                "updated_at": "2026-04-08T15:30:00Z"
            }
        ],
        "total": 1,
        "page": 1,
        "page_size": 20
    }
}
```

**获取模型详情**

```
GET /api/v1/models/{id}

响应: 200 OK
{
    "code": 200,
    "data": {
        "id": 1,
        "model_code": "LUNG_NODULE_DET_001",
        "model_name": "肺结节检测模型",
        "description": "...",
        "model_type": "IMAGING",
        "task_type": "OBJECT_DETECTION",
        "framework": "PYTORCH",
        "input_schema": { },
        "output_schema": { },
        "tags": ["肺部", "CT"],
        "status": "PUBLISHED",
        "owner": { "id": 1, "name": "张三" },
        "project": { "id": 1, "name": "AI辅助诊断研究" },
        "version_count": 3,
        "latest_version": {
            "version_no": "v1.2.0",
            "status": "APPROVED",
            "created_at": "2026-04-07T09:00:00Z"
        },
        "active_deployment": {
            "id": 1,
            "status": "RUNNING",
            "endpoint_url": "http://maidc-aiworker:8090/v1/infer/lung-nodule"
        },
        "statistics": {
            "total_inferences": 15234,
            "avg_latency_ms": 245,
            "last_24h_inferences": 567
        },
        "created_at": "2026-04-01T10:00:00Z",
        "updated_at": "2026-04-08T15:30:00Z"
    }
}
```

---

#### 6.2.2 版本管理

**创建版本**

```
POST /api/v1/models/{id}/versions
Content-Type: multipart/form-data

表单字段:
- version_no: "v1.3.0"           (必填)
- description: "优化小结节检测"    (可选)
- changelog: "1. 优化..."        (可选)
- model_file: <文件上传>          (必填, .pt/.onnx/.pkl)
- config_file: <文件上传>         (可选)
- hyper_params: JSON字符串         (可选)

响应: 201 Created
{
    "code": 201,
    "data": {
        "id": 10,
        "model_id": 1,
        "version_no": "v1.3.0",
        "status": "CREATED",
        "model_file_size": 524288000,
        "upload_progress": 100
    }
}
```

**版本列表与对比**

```
GET /api/v1/models/{id}/versions?page=1&page_size=10

GET /api/v1/models/{id}/versions/compare?v1=3&v2=5
响应包含两个版本的指标对比、参数差异、评估结果
```

---

#### 6.2.3 模型评估

**创建评估任务**

```
POST /api/v1/evaluations
Content-Type: application/json

{
    "model_version_id": 10,
    "eval_name": "v1.3.0外部验证集评估",
    "eval_type": "EXTERNAL",
    "dataset_id": 5,
    "metrics_config": {
        "metrics": ["auc", "f1", "precision", "recall", "sensitivity", "specificity"],
        "confidence_threshold": 0.5,
        "iou_threshold": 0.3
    }
}

响应: 201 Created
{
    "code": 201,
    "data": {
        "id": 20,
        "status": "PENDING",
        "estimated_duration_min": 30
    }
}
```

**评估结果**

```
GET /api/v1/evaluations/{id}

响应: 200 OK
{
    "code": 200,
    "data": {
        "id": 20,
        "eval_name": "v1.3.0外部验证集评估",
        "status": "COMPLETED",
        "metrics": {
            "auc": 0.9234,
            "f1": 0.8912,
            "precision": 0.9045,
            "recall": 0.8786,
            "sensitivity": 0.8786,
            "specificity": 0.9512
        },
        "confusion_matrix": {
            "TP": 442, "FP": 46,
            "FN": 61, "TN": 951
        },
        "started_at": "2026-04-08T10:00:00Z",
        "finished_at": "2026-04-08T10:28:45Z",
        "dataset": { "id": 5, "name": "外部验证集-2026Q1", "record_count": 1500 },
        "report_url": "/api/v1/evaluations/20/report"
    }
}
```

---

#### 6.2.4 审批流程

**提交审批**

```
POST /api/v1/approvals
{
    "model_version_id": 10,
    "approval_type": "DEPLOY",
    "evidence_docs": [
        {"name": "评估报告v1.3.pdf", "path": "/docs/eval_v130.pdf", "type": "EVALUATION"},
        {"name": "临床验证报告.pdf", "path": "/docs/clinical.pdf", "type": "CLINICAL"}
    ],
    "risk_assessment": "低风险，AUC>0.92，误诊率<5%",
    "clinical_validation": "已完成500例回顾性验证"
}

响应: 201 Created
{
    "code": 201,
    "data": {
        "id": 5,
        "status": "PENDING",
        "applicant": {"id": 1, "name": "张三"},
        "reviewer": {"id": 2, "name": "李主任"},
        "submitted_at": "2026-04-08T14:00:00Z"
    }
}
```

**审批操作**

```
PUT /api/v1/approvals/{id}/review
{
    "result": "APPROVED",
    "result_comment": "模型性能达标，同意上线部署"
}

响应: 200 OK
```

---

#### 6.2.5 部署管理

**创建部署**

```
POST /api/v1/deployments
{
    "model_version_id": 10,
    "deployment_name": "肺结节检测-生产环境",
    "deployment_type": "ONLINE",
    "serving_framework": "TRITON",
    "resource_config": {
        "cpu": "4",
        "memory": "16Gi",
        "gpu": 1,
        "gpu_type": "NVIDIA_T4"
    },
    "env_vars": {
        "MODEL_BATCH_SIZE": "8",
        "MODEL_MAX_LATENCY_MS": "500"
    },
    "auto_scale": true,
    "min_replicas": 1,
    "max_replicas": 5
}

响应: 201 Created
{
    "code": 201,
    "data": {
        "id": 3,
        "status": "CREATING",
        "endpoint_url": null
    }
}
```

**部署状态与操作**

```
GET    /api/v1/deployments/{id}/status       # 查询状态
PUT    /api/v1/deployments/{id}/start         # 启动
PUT    /api/v1/deployments/{id}/stop          # 停止
PUT    /api/v1/deployments/{id}/scale         # 扩缩容
POST   /api/v1/deployments/{id}/restart       # 重启
```

**扩缩容**

```
PUT /api/v1/deployments/{id}/scale
{
    "target_replicas": 3,
    "resource_config": {
        "cpu": "8",
        "memory": "32Gi",
        "gpu": 2
    }
}
```

---

#### 6.2.6 流量路由

**创建金丝雀发布路由**

```
POST /api/v1/routes
{
    "route_name": "肺结节检测-金丝雀发布",
    "model_id": 1,
    "route_type": "CANARY",
    "default_deployment_id": 1,
    "config": [
        {"deployment_id": 1, "weight": 90},
        {"deployment_id": 3, "weight": 10}
    ],
    "traffic_rules": {
        "canary_percentage": 10,
        "success_threshold": 0.95,
        "auto_promote": false
    }
}
```

---

#### 6.2.7 推理与监控

**同步推理调用**

```
POST /api/v1/inference/{deployment_id}
Content-Type: application/json

{
    "request_id": "req-uuid-001",
    "patient_id": 12345,
    "encounter_id": 67890,
    "input": {
        "image_url": "minio://maidc/dicom/2026/04/08/CT001.dcm",
        "parameters": {
            "confidence_threshold": 0.5,
            "nms_threshold": 0.3
        }
    }
}

响应: 200 OK
{
    "code": 200,
    "data": {
        "request_id": "req-uuid-001",
        "results": [
            {
                "bbox": [120.5, 85.3, 12.7, 11.2, 8.5],
                "confidence": 0.94,
                "label": "nodule",
                "classification": "suspicious",
                "volume_mm3": 1247.8
            }
        ],
        "latency_ms": 234,
        "model_version": "v1.3.0"
    }
}
```

**查询推理日志**

```
GET /api/v1/monitoring/deployments/{id}/logs?page=1&page_size=50&start_time=2026-04-08T00:00:00Z&end_time=2026-04-08T23:59:59Z&status=ERROR

响应: 200 OK（分页返回推理日志列表）
```

**查询运行指标**

```
GET /api/v1/monitoring/deployments/{id}/metrics?metric_name=latency_p99&start_time=...&end_time=...&interval=5m

响应: 200 OK
{
    "code": 200,
    "data": {
        "metric_name": "latency_p99",
        "interval": "5m",
        "data_points": [
            {"time": "2026-04-08T10:00:00Z", "value": 312.5},
            {"time": "2026-04-08T10:05:00Z", "value": 298.3}
        ]
    }
}
```

---

#### 6.2.8 告警管理

**创建告警规则**

```
POST /api/v1/alert-rules
{
    "rule_name": "推理延迟告警",
    "deployment_id": 3,
    "metric_name": "latency_p99",
    "condition": "GT",
    "threshold": 500,
    "duration_sec": 300,
    "severity": "WARNING",
    "notify_channels": ["email", "webhook"],
    "notify_users": [1, 2, 3]
}
```

**告警操作**

```
GET    /api/v1/alerts?status=FIRING            # 活跃告警
PUT    /api/v1/alerts/{id}/acknowledge          # 确认告警
GET    /api/v1/alerts/history?rule_id=1         # 告警历史
```

`★ Insight ─────────────────────────────────────`
**API设计模式**: 模型管理API遵循RESTful资源嵌套模式 `models/{id}/versions/{vid}`，同时推理接口走独立路径 `/inference/{deployment_id}` 直接打到部署实例。审批流程采用状态机模式，部署管理采用声明式配置（提交期望状态，系统自动收敛）。这种分离确保了管理面和数据面的独立演进。
`─────────────────────────────────────────────────`

---

## 7. 分期实施计划

### 7.1 Phase 1 - 模型管理核心（8周）

**目标**: 搭建基础架构，实现模型全生命周期管理

| 周次 | 交付物 | 说明 |
|------|--------|------|
| W1-2 | 基础设施 | Docker Compose部署；PostgreSQL+Nacos+MinIO+Redis+RabbitMQ |
| W1-2 | 数据库初始化 | model schema建表；system schema基础表 |
| W3-4 | maidc-model v1 | 模型注册、版本管理、文件上传 |
| W3-4 | maidc-auth v1 | 用户管理、JWT认证、RBAC权限 |
| W5-6 | Spring Cloud Gateway | 路由配置、JWT鉴权过滤器 |
| W5-6 | 模型评估与审批 | 评估任务、审批流程 |
| W7-8 | maidc-aiworker v1 | FastAPI推理服务、Celery评估Worker |
| W7-8 | 模型部署与监控 | 部署管理、推理日志、基础告警 |

**Phase 1 完成标准**:
- 模型可通过API完成注册→版本→评估→审批→部署全流程
- 推理API可正常调用并返回结果
- 基础监控面板可查看推理QPS和延迟

### 7.2 Phase 2 - 数据管理（8周）

**目标**: 建立CDR/RDR数据管线，实现临床数据到科研数据的转换

| 周次 | 交付物 | 说明 |
|------|--------|------|
| W1-2 | cdr schema建表 | 28张CDR表 |
| W1-2 | 数据源适配器 | HIS/PACS/LIS数据接入接口 |
| W3-4 | maidc-data CDR模块 | 患者管理、就诊数据管理 |
| W3-4 | 数据脱敏 | 患者敏感信息加密/脱敏查询 |
| W5-6 | rdr schema建表 | 19张RDR表 |
| W5-6 | maidc-data RDR模块 | 项目管理、队列定义、数据集管理 |
| W7-8 | ETL引擎 | 数据提取、转换、加载管线 |
| W7-8 | 数据质量 | 质量规则配置、自动检测 |

### 7.3 Phase 3 - 任务调度与AI集群（6周）

**目标**: 完善异步调度能力，扩展AI Worker集群

| 周次 | 交付物 | 说明 |
|------|--------|------|
| W1-2 | maidc-task | XXL-JOB集成、定时任务管理 |
| W1-2 | Worker集群扩展 | GPU调度、多Worker节点管理 |
| W3-4 | 高级评估 | 交叉验证、A/B测试、模型对比 |
| W3-4 | 流量路由 | 金丝雀发布、加权路由 |
| W5-6 | Prometheus+Grafana | 监控面板、资源告警 |
| W5-6 | ELK日志 | 集中式日志收集与查询 |

### 7.4 Phase 4 - 标注与审计（6周）

**目标**: 数据标注能力与合规审计体系

| 周次 | 交付物 | 说明 |
|------|--------|------|
| W1-3 | maidc-label | 影像标注、文本标注、标注质量管理 |
| W1-3 | maidc-audit | 操作审计、数据访问审计、合规报表 |
| W4-5 | maidc-msg | 站内消息、邮件、Webhook通知 |
| W4-5 | 标注工具集成 | AI辅助预标注、多人协同 |
| W6 | 联调测试 | 服务间集成测试、端到端验证 |

### 7.5 Phase 5 - Portal与集成（8周）

**目标**: 统一前端，全链路集成，合规加固

| 周次 | 交付物 | 说明 |
|------|--------|------|
| W1-3 | Web Portal | Vue3前端，模型管理/数据管理/监控页面 |
| W3-5 | 全链路集成 | 端到端流程打通（数据接入→标注→训练→部署→推理） |
| W5-6 | 安全加固 | 等保三级合规、渗透测试、安全审计 |
| W6-7 | 性能优化 | 接口性能、数据库调优、缓存策略 |
| W7-8 | 验收交付 | 文档完善、培训材料、上线支持 |

### 7.6 总体时间线

```
Phase 1: ████ 8周  模型管理核心
Phase 2:     ████ 8周  数据管理
Phase 3:         ███ 6周  任务调度与AI集群
Phase 4:            ███ 6周  标注与审计
Phase 5:                ████ 8周  Portal与集成
总计: ~36周（约9个月）
```

---

## 8. 安全与合规

### 8.1 安全架构

采用5层安全防护体系：

```
┌─────────────────────────────────────────┐
│            网络层安全                     │  VPC隔离 / 安全组 / DDoS防护
├─────────────────────────────────────────┤
│            传输层安全                     │  TLS 1.3 / 双向认证
├─────────────────────────────────────────┤
│            应用层安全                     │  JWT / OAuth2 / RBAC / API限流
├─────────────────────────────────────────┤
│            数据层安全                     │  字段加密 / 数据脱敏 / 匿名化
├─────────────────────────────────────────┤
│            存储层安全                     │  磁盘加密 / MinIO服务端加密 / 备份
└─────────────────────────────────────────┘
```

### 8.2 数据安全措施

| 措施 | 说明 |
|------|------|
| **敏感字段加密** | 身份证号、姓名、电话、地址使用AES-256加密存储 |
| **数据脱敏** | 列表查询返回脱敏数据（姓名→张**, 电话→138****1234） |
| **匿名化** | 科研数据集发布前自动去标识化（移除直接标识符） |
| **数据权限** | 基于org_id的行级数据隔离，RBAC控制访问范围 |
| **审计追踪** | 全链路操作日志，数据访问记录可追溯 |
| **传输加密** | 全站HTTPS（TLS 1.3），内部服务间mTLS |

### 8.3 合规要求（预留）

> 注：本章为合规设计预留框架，具体实施需结合等保三级评估结果调整。

| 合规领域 | 要求 | MAIDC对应措施 |
|----------|------|---------------|
| 等保三级 | 身份鉴别、访问控制、安全审计 | JWT+RBAC+审计日志 |
| 个人信息保护法 | 最小必要、知情同意、可删除 | 数据脱敏+匿名化+数据主体权利 |
| 医疗数据管理 | 数据分类分级、安全存储 | 5层安全体系+加密存储 |
| 人类遗传资源管理 | 审批备案、出境管理 | 审批流程+数据访问控制 |

---

## 9. 部署架构

### 9.1 Phase 1 - Docker Compose 单机部署

适用于开发测试和小规模试点（1-3家医院）。

```yaml
# docker-compose.yml 核心结构
version: '3.8'
services:
  # 基础设施
  postgres:
    image: postgres:15
  redis:
    image: redis:7-alpine
  minio:
    image: minio/minio
  rabbitmq:
    image: rabbitmq:3-management
  nacos:
    image: nacos/nacos-server:v2.3

  # 微服务
  maidc-gateway:
    build: ./maidc-gateway
    ports: ["8080:8080"]
  maidc-auth:
    build: ./maidc-auth
  maidc-model:
    build: ./maidc-model
  maidc-aiworker:
    build: ./maidc-aiworker

  # 监控
  prometheus:
    image: prom/prometheus
  grafana:
    image: grafana/grafana
```

**资源需求**（最小配置）:

| 组件 | CPU | 内存 | 存储 |
|------|-----|------|------|
| PostgreSQL | 2C | 4GB | 100GB SSD |
| Redis | 1C | 2GB | - |
| MinIO | 2C | 4GB | 500GB |
| RabbitMQ | 1C | 2GB | - |
| Nacos | 1C | 2GB | - |
| Java服务x3 | 3C | 6GB | - |
| AI Worker | 2C+1GPU | 8GB | 50GB |
| 监控 | 1C | 2GB | 50GB |
| **合计** | **13C+1GPU** | **30GB** | **700GB** |

### 9.2 Phase 3+ - Kubernetes 集群部署

适用于生产环境和多中心部署（5-20家医院）。

```
┌──────────────────────────────────────────────────┐
│                 Kubernetes Cluster                │
│                                                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │ Ingress   │ │ Gateway  │ │ Nacos            │  │
│  │ Controller│ │ Pod x2   │ │ Pod x3           │  │
│  └──────────┘ └──────────┘ └──────────────────┘  │
│                                                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐         │
│  │ auth     │ │ model    │ │ data     │ ...     │
│  │ Pod x2   │ │ Pod x3   │ │ Pod x3   │         │
│  └──────────┘ └──────────┘ └──────────┘         │
│                                                   │
│  ┌─────────────────────────────────────────────┐ │
│  │ GPU Node Pool                                │ │
│  │ ┌───────────┐ ┌───────────┐ ┌───────────┐  │ │
│  │ │ aiworker  │ │ aiworker  │ │ aiworker  │  │ │
│  │ │ GPU: T4   │ │ GPU: T4   │ │ GPU: A100 │  │ │
│  │ └───────────┘ └───────────┘ └───────────┘  │ │
│  └─────────────────────────────────────────────┘ │
│                                                   │
│  ┌─────────┐ ┌─────┐ ┌───────┐ ┌──────────────┐ │
│  │ PG集群   │ │Redis│ │MinIO  │ │ RabbitMQ集群  │ │
│  │ 主从     │ │集群  │ │集群   │ │               │ │
│  └─────────┘ └─────┘ └───────┘ └──────────────┘ │
└──────────────────────────────────────────────────┘
```

**K8s资源规划**:

| 组件 | 副本 | CPU | 内存 | 存储 |
|------|------|-----|------|------|
| Gateway | 2 | 1C | 1Gi | - |
| auth | 2 | 1C | 1Gi | - |
| model | 3 | 2C | 2Gi | - |
| data | 3 | 2C | 2Gi | - |
| task | 2 | 1C | 1Gi | - |
| label | 2 | 1C | 1Gi | - |
| audit | 2 | 1C | 1Gi | - |
| msg | 2 | 1C | 1Gi | - |
| aiworker(GPU) | 3 | 4C+1GPU | 8Gi | - |
| PostgreSQL | 主+2从 | 4C | 16Gi | 1TB SSD |
| Redis | 3哨兵 | 2C | 8Gi | - |
| MinIO | 4节点 | 2C | 4Gi | 5TB |
| RabbitMQ | 3节点 | 2C | 4Gi | - |
| Nacos | 3节点 | 1C | 2Gi | - |

---

## 10. 附录

### 10.1 术语表

| 缩写 | 全称 | 说明 |
|------|------|------|
| MAIDC | Medical AI Data Center | 医疗AI数据中心 |
| CDR | Clinical Data Repository | 临床数据仓库 |
| RDR | Research Data Repository | 研究数据仓库 |
| ETL | Extract-Transform-Load | 数据抽取-转换-加载 |
| HIS | Hospital Information System | 医院信息系统 |
| PACS | Picture Archiving and Communication System | 影像归档与通信系统 |
| LIS | Laboratory Information System | 检验信息系统 |
| ICD | International Classification of Diseases | 国际疾病分类 |
| LOINC | Logical Observation Identifiers Names and Codes | 检验指标编码标准 |
| DICOM | Digital Imaging and Communications in Medicine | 医学数字成像通信标准 |
| HL7 | Health Level Seven | 医疗信息交换标准 |
| FHIR | Fast Healthcare Interoperability Resources | 快速医疗互操作资源 |
| OMOP | Observational Medical Outcomes Partnership | 观察性医疗结果合作伙伴（通用数据模型） |
| RBAC | Role-Based Access Control | 基于角色的访问控制 |
| JWT | JSON Web Token | JSON网络令牌 |
| WGS | Whole Genome Sequencing | 全基因组测序 |
| WES | Whole Exome Sequencing | 全外显子测序 |
| VCF | Variant Call Format | 变异调用格式 |
| NIfTI | Neuroimaging Informatics Technology Initiative | 神经影像信息格式 |
| ROI | Region of Interest | 感兴趣区域 |
| NMS | Non-Maximum Suppression | 非极大值抑制 |
| mTLS | Mutual TLS | 双向TLS认证 |

### 10.2 数据库Schema统计

| Schema | 表数量 | 说明 |
|--------|--------|------|
| cdr | 28 | 临床数据仓库 |
| rdr | 19 | 研究数据仓库 |
| model | 10 | 模型管理 |
| system | 7 | 系统管理 |
| audit | 3 | 审计日志 |
| **合计** | **67** | |

### 10.3 微服务端口分配

| 服务 | 端口 | 说明 |
|------|------|------|
| maidc-gateway | 8080 | API网关 |
| maidc-auth | 8081 | 用户权限 |
| maidc-data | 8082 | 数据管理 |
| maidc-model | 8083 | 模型管理 |
| maidc-task | 8084 | 任务调度 |
| maidc-label | 8085 | 数据标注 |
| maidc-audit | 8060 | 审计日志 |
| maidc-msg | 8087 | 消息通知 |
| maidc-aiworker | 8090 | AI推理 |

### 10.4 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-04-08 | 初始版本，完成全部设计章节 |

---

> **文档结束** - MAIDC Medical AI Data Center PRD v1.0
