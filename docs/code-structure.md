# MAIDC 代码结构说明

> **Medical AI Data Center** — 医疗AI数据中心平台
> 本文档描述整个仓库的代码组织结构、模块职责与技术栈，供新成员快速了解项目全貌。
>
> 更新日期：2026-09-08

---

## 1. 项目概览

| 维度 | 说明 |
|------|------|
| 项目定位 | 医疗AI模型全生命周期管理平台（数据接入 → 标准化 → 科研检索 → 标注 → 训练 → 部署 → 审计） |
| 后端 | Java 17 + Spring Boot 3.2.5 + Spring Cloud 2023.0.1（Alibaba）微服务架构 |
| 前端 | Vue 3.4 + TypeScript + Vite 5 + Ant Design Vue 4 + Pinia + ECharts |
| AI 推理 | Python FastAPI + Celery 异步任务 |
| 数据层 | PostgreSQL 15（ODS/CDR/RDR 多 Schema）+ Redis 7 + MinIO + RabbitMQ 3.12 + Nacos 2.3 |
| 代码规模 | Java 约 754 个文件、Vue 约 165 个、TS 约 38 个、Python 11 个、SQL 初始化脚本 19 个 |

### 数据分层架构（医疗数仓经典三层）

```
ODS（原始层）──ETL──▶ CDR（临床数据仓库）──抽取──▶ RDR（科研数据仓库）
  mimic3/mimic4 等        患者360/临床检索/             队列/数据集/
  原始镜像数据            质控/术语映射                 特征字典/多模态
```

---

## 2. 顶层目录结构

```
e:\pxg_project\
├── maidc-parent/          # 后端 Maven 多模块工程（8个Java微服务 + 1个Python服务）
├── maidc-portal/          # 前端 Vue3 单页应用
├── ai-worker/             # 早期 Python 原型（仅 main.py，已被 maidc-aiworker 取代）
├── docker/                # Docker Compose 编排 + 数据库初始化脚本
├── docs/                  # 项目文档（设计稿、部署指南、SQL、规范）
├── monitoring/            # 监控配置
├── scripts/               # 运维/数据导入脚本（embulk、CDR导入、e2e测试）
├── standards/             # 卫生行业标准（WS/T 363、364、ICD10 词典等）
├── openspec/              # OpenSpec 变更管理
├── data/                  # 数据文件
├── CLAUDE.md / AGENTS.md  # AI 助手协作指引
└── README.md              # 项目简介
```

---

## 3. 后端 `maidc-parent/`（Spring Cloud 微服务）

### 3.1 工程结构

```
maidc-parent/
├── pom.xml                # 父 POM：统一依赖版本管理（Java 17 / Boot 3.2.5 / Cloud 2023.0.1）
├── common/                # 公共库模块（被各服务依赖）
│   ├── common-core/       #   统一返回 R / PageResult、ErrorCode、BusinessException、全局异常
│   ├── common-redis/      #   Redis 配置、缓存服务、分布式锁、缓存策略
│   ├── common-minio/      #   MinIO 对象存储配置与服务
│   ├── common-mq/         #   RabbitMQ 基础配置、MaidcMessage 模型、生产/消费基类
│   ├── common-log/        #   @OperLog 注解 + AOP 操作日志切面（异步事件发布）
│   ├── common-security/   #   JWT 工具、CurrentUserId 注解解析、XSS 过滤、脱敏、AES 加密、密码策略
│   └── common-jpa/        #   BaseEntity、JPA 配置、JsonNode 转换器
├── maidc-gateway/         # API 网关（路由、鉴权过滤器）
├── maidc-auth/            # 认证授权服务
├── maidc-data/            # 数据服务（最大模块：主数据/CDR/RDR/ETL/智能检索）
├── maidc-model/           # 模型服务（模型全生命周期 + 生命周期告警）
├── maidc-task/            # 任务调度服务
├── maidc-label/           # 标注服务
├── maidc-audit/           # 审计服务
├── maidc-msg/             # 消息服务（站内信 + WebSocket 推送）
├── maidc-aiworker/        # AI 推理服务（Python，不在 Maven modules 中，独立 pyproject.toml）
├── docker/                # 服务镜像构建
├── start-all-services.bat # Windows 一键启动脚本
└── STARTUP.md             # 启动说明
```

### 3.2 微服务端口与职责一览

| 服务 | 端口 | 核心职责 | 主要 Controller |
|------|------|----------|----------------|
| **maidc-gateway** | 8080 | API 网关：路由转发、JWT 鉴权过滤器 | （filter 包：网关过滤器） |
| **maidc-auth** | 8081 | 登录认证、用户/角色/权限、系统配置 | Auth / User / Role / SystemConfig |
| **maidc-data** | 8082 | 主数据管理、CDR/RDR 数据服务、ETL 管道、临床检索 | 50+ 个（见 3.3） |
| **maidc-model** | 8083 | 模型管理、版本、评估、审批、部署、推理、监控、告警 | Model / Version / Evaluation / Approval / Deployment / Inference / Monitoring / Alert |
| **maidc-task** | 8084 | 通用任务调度、个人工作台 | Task / Workspace |
| **maidc-label** | 8085 | 标注任务分发与工作台 | LabelTask |
| **maidc-audit** | 8086 | 操作日志、数据访问日志、系统事件、合规报告 | Audit |
| **maidc-msg** | 8087 | 站内消息、通知设置、消息模板（WebSocket 实时推送） | Message / Notification |

> 注意：`maidc-model` 模块内含 `com.maidc.label` 包（标注任务相关），与独立的 `maidc-label` 服务存在包名复用，阅读代码时需区分两者。

### 3.3 `maidc-data` 模块详解（按包划分）

```
com.maidc.data
├── controller/（+ entity/repository/service 同构分包）
│   ├── 主数据（WS/T 363 标准）：ConceptDomain / ValueDomain / DataElementConcept /
│   │   DataElement / CodeSystem / Concept / ConceptMapping / ObjectClass / Property
│   ├── 医学字典：Diagnosis(ICD) / Drug / LabItem / ExamItem / FeeItem / Dict
│   ├── CDR 临床数据仓库：PatientEncounter / Patient360 / ClinicalSearch / SmartSearch /
│   │   DiseaseCohort / DiseaseTemplate / Knowledge / DrugInteraction / ReferenceRange
│   ├── cdr/ 质控与治理：QualityRule / QualityCheck / Quarantine / TermMapping / DataLineage
│   ├── ETL 管道：EtlPipeline / EtlStep / EtlFieldMapping / EtlExecution / EtlImport / EtlMetadata
│   ├── RDR 科研数据仓库：RdrSelection / RdrDatasetVersion / RdrExtractionTask /
│   │   RdrExtractionExecution / RdrMultimodal
│   ├── 数据源与脱敏：DataSource / DataSourceType / DesensitizeRule / SyncTask / MasterDataImport
│   └── 其他：Institution / Mapping / Task / Workspace
├── etl/                   # ETL 引擎核心逻辑
└── service/connection/    # 外部数据源连接管理
```

### 3.4 各服务的统一分层约定

每个 Java 服务均遵循标准分层：

```
controller/   → REST API 入口（springdoc 自动生成 OpenAPI 文档）
service/      → 业务逻辑（含 impl/）
repository/   → Spring Data JPA 数据访问
mapper/       # MapStruct 对象转换（DTO ↔ Entity ↔ VO）
entity/       → JPA 实体（均继承 common-jpa 的 BaseEntity，不使用外键约束）
dto/  vo/     → 请求/响应对象
mq/ listener/ consumer/ → RabbitMQ 异步消息
config/       → 模块配置（SecurityConfig 等）
```

---

## 4. AI 推理服务 `maidc-parent/maidc-aiworker/`（Python）

```
maidc-aiworker/
├── app/
│   ├── main.py                # FastAPI 入口
│   ├── api/
│   │   ├── health.py          # 健康检查
│   │   ├── inference.py       # 推理接口
│   │   ├── serving.py         # 模型服务化
│   │   └── workers.py         # Worker 状态查询
│   ├── core/
│   │   ├── celery_app.py      # Celery 应用（异步任务队列）
│   │   └── config.py          # 配置
│   └── tasks/
│       ├── preprocessing.py   # 数据预处理任务
│       ├── inference_batch.py # 批量推理任务
│       └── evaluation.py      # 模型评估任务
├── Dockerfile
└── pyproject.toml
```

由 `maidc-model` 服务通过 RabbitMQ 下发任务，Celery Worker 异步执行训练/推理/评估。

---

## 5. 前端 `maidc-portal/`（Vue 3 SPA）

### 5.1 技术栈

Vue 3.4（Composition API）+ TypeScript 5.4 + Vite 5 + Ant Design Vue 4 + Pinia + Vue Router 4 + Axios + ECharts/vue-echarts + Vue Flow（ETL 流程画布）+ Sass

### 5.2 `src/` 结构

```
maidc-portal/src/
├── main.ts / App.vue
├── api/                 # 后端接口封装（按服务域拆分）
│   ├── auth.ts system.ts audit.ts data.ts etl.ts label.ts model.ts msg.ts task.ts
│   ├── workspace.ts masterdata.ts medical-dictionary.ts dataElementStandard.ts
│   └── cdr/patientEncounter.ts
├── router/
│   ├── index.ts         # 路由装配
│   ├── constantRoutes.ts# 公共路由（登录、重定向、错误页）
│   ├── asyncRoutes.ts   # 权限动态路由（按菜单模块组织）
│   └── guards.ts        # 导航守卫（登录态/权限校验、NProgress）
├── stores/              # Pinia 状态
│   ├── auth.ts（登录态） permission.ts（动态路由） ui.ts workspace.ts
│   └── cdr/patientEncounter.ts
├── layouts/             # 全局布局（侧边栏/顶栏框架）
├── hooks/               # 组合式函数
├── utils/               # 工具函数（axios 封装等）
├── types/               # TS 类型定义
├── assets/styles/       # 静态资源与样式
├── components/          # 通用组件（30+，见 5.4）
└── views/               # 页面（按业务模块组织，见 5.3）
```

### 5.3 页面模块 `views/`

| 目录 | 功能 | 代表页面 |
|------|------|----------|
| `login/` | 登录 | LoginPage |
| `dashboard/` | 仪表盘 | Overview（总览）、DataDashboard（数据）、ModelDashboard（模型） |
| `data-cdr/` | CDR 临床数据（最大模块） | PatientList/PatientDetail、EncounterDetail、ClinicalSearch、SmartSearch 相关、DiseaseList/DiseaseDetail（病种队列）、LabResultView/MedicationView/VitalSignView/ImagingView/ClinicalNoteView/DiagnosisView（临床视图）、QualityRuleList/QualityResultList（质控）、DataSourceList、SyncTaskList、DesensitizeRule、DictManage |
| `data-etl/` | ETL 可视化编排 | EtlPipelineList/Config、EtlExecutionList、EtlCanvas/EtlDesigner/EtlPalette（Vue Flow 画布）、EtlNode、FieldMappingModal |
| `data-rdr/` | RDR 科研数据 | ProjectList/Detail、CohortList、DatasetList/Detail、EtlTaskList/Detail、FeatureDictionary |
| `model/` | 模型全生命周期 | ModelList/Detail、VersionList、EvalList/Detail、ApprovalList、DeploymentList/Detail、RouteConfig、InferenceLog |
| `label/` | 数据标注 | LabelTaskList/Detail、LabelWorkspace、LabelWorkspaceText |
| `audit/` | 审计 | OperationLog、DataAccessLog、SystemEventLog、ComplianceReport |
| `alert/` | 告警 | AlertList/Detail、AlertRuleList |
| `schedule/` | 任务调度 | TaskList |
| `message/` | 消息中心 | MessageList/Detail、NotificationSettings、TemplateManagement |
| `masterdata/` | 主数据管理（WS/T 363） | ConceptDomainList、ValueDomainList、DataElementConceptList、DataElementList、CodeSystems、MappingManager、DomainManager、dictionary/（诊断/药品/检验/检查/收费5类字典）、KnowledgeList、InstitutionList、ClinicalRules + 15 个表单/抽屉组件 |
| `system/` | 系统管理 | UserList/Detail、RoleList/Detail/RolePermission、PermissionManagement、OrganizationList/Detail、SystemConfig |
| `error/` `redirect/` | 错误页/重定向 | — |

### 5.4 通用组件 `components/`（30+）

- **业务型**：PatientInfoCard、DiseaseCard、MetricCard/MetricChart、ConfusionMatrix、RocCurve（模型评估）、DatasetSelect、ModelSelect、UserSelect、ApprovalTimeline、DeploymentStatus、HealthMonitor
- **工具型**：SearchForm、PageContainer、EmptyState、StatusBadge、VersionTag、CountDown、Pagination
- **编辑器/查看器**：CodeEditor、JsonViewer、SchemaViewer、DiffViewer、PdfViewer、ImagePreview、ConditionBuilder（条件构造器）、DynamicFormRenderer、KeyValueEditor、TrafficRuleEditor（灰度路由）、ResourceConfigForm、DesensitizePreview（脱敏预览）、FileUploader
- **权限**：PermissionWrapper（按钮级权限控制）

---

## 6. 基础设施 `docker/`

### 6.1 Compose 编排文件

| 文件 | 用途 |
|------|------|
| `docker-compose-infra.yml` | 仅中间件：postgres(5432) / redis(6379) / minio(9000,9001) / rabbitmq(5672,15672) / nacos(8848) |
| `docker-compose-full.yml` | 中间件 + 全部 8 个 Java 微服务容器 |
| `docker-compose-monitoring.yml` | 监控栈 |

### 6.2 数据库初始化 `docker/init-db/`（19 个 SQL，按序号执行）

```
01-schemas.sql           # 创建各 Schema
02-system.sql            # auth 系统库（用户/角色/权限/配置）
03-model.sql             # 模型库
04-cdr.sql               # CDR 临床数据仓库
05-rdr.sql               # RDR 科研数据仓库
06-audit.sql             # 审计库
07-ods-schema.sql        # ODS 原始层 Schema
08/09-ods-mimic*.sql     # MIMIC-III / MIMIC-IV 数据导入
10-cdr-patch.sql         # CDR 补丁
11-ods-reduce.sql        # ODS 精简
12-cdr-etl.sql           # CDR ETL 相关表
13-cdr-etl-edge.sql      # ETL 扩展 + 数据源类型
14-smart-search-fts.sql  # 智能检索全文索引
15-disease-cohort.sql    # 病种队列
16-personal-task.sql     # 个人任务
```

---

## 7. 其他目录

| 目录 | 内容 |
|------|------|
| `scripts/` | embulk 数据导入工具、`import_cdr_data.py` CDR 数据导入、e2e 测试脚本、PostgreSQL 空间回收（vacuum/compact） |
| `standards/` | 卫生行业标准 PDF（WS/T 363 各册、WS/T 364 值域代码、WS/T 303 数据元规则、ICD10 词典 CSV），主数据模块的数据标准依据 |
| `docs/` | `deployment-guide.md` 部署指南、`data-element-standard-model.md` 数据元标准模型、`masterdata-ui-design.md`、`security/`、`sql/`、`specs/` |
| `monitoring/` | 监控配置 |
| `openspec/` | OpenSpec 规范化变更管理 |

---

## 8. 整体架构图

```
                        ┌──────────────────────┐
                        │   maidc-portal (Vue3) │  :5173 dev
                        └──────────┬───────────┘
                                   │ HTTP
                        ┌──────────▼───────────┐
                        │  maidc-gateway  :8080 │  JWT 鉴权 / 路由
                        └──┬────┬────┬────┬────┬─┬────┬────┬──┘
        ┌─────────────────┘    │    │    │    │ │    │    └──────────────┐
   ┌────▼────┐           ┌────▼──┐ │ ┌──▼──┐│┌─▼───┐ │ ┌──▼───┐    ┌────▼────┐
   │auth:8081│           │data   │ │ │model│││task │ │ │audit │    │  msg    │
   │认证/用户 │           │:8082  │ │ │:8083│││:8084│ │ │:8086 │    │:8087    │
   └─────────┘           │主数据  │ │ │模型  │││调度 │ │ │审计  │    │WebSocket│
                         │CDR/RDR│ │ │生命周期││└────┘ │ └──────┘    └─────────┘
                         │ETL    │ │ └──┬──┘│
                         └───────┘ │    │ RabbitMQ
                                   │ ┌──▼──────────────┐
                                   │ │ maidc-aiworker  │
                                   │ │ FastAPI + Celery │
                                   │ └─────────────────┘
   ┌───────────────────── 中间件 ─────────────────────┐
   │ PostgreSQL:5432 │ Redis:6379 │ MinIO:9000 │      │
   │ RabbitMQ:5672   │ Nacos:8848 │   label:8085      │
   └──────────────────────────────────────────────────┘
```

---

## 9. 本地启动速查

```bash
# 1. 启动中间件
cd docker && docker compose -f docker-compose-infra.yml up -d

# 2. 启动后端（方式一：编译后逐服务启动）
cd maidc-parent && mvn clean package -DskipTests
# 方式二：Windows 一键脚本
maidc-parent\start-all-services.bat

# 3. 启动前端
cd maidc-portal && npm install && npm run dev
```

详细说明见 `maidc-parent/STARTUP.md` 与 `docs/deployment-guide.md`。
