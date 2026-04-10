# MAIDC 研发计划

> **版本**: v1.0
> **日期**: 2026-04-11
> **周期**: Phase 1 ≈ 8 周 | 全量 ≈ 36 周
> **UI 基准**: `pencil-new.pen` 设计稿（55 页 + 27 弹窗 + 44 组件）
> **提交策略**: 每个任务完成一次 git commit

---

## 总体原则

1. **UI 一致性**: 所有页面严格对齐 `pencil-new.pen` 设计稿，组件使用 44 个可复用组件
2. **增量可运行**: 每个 commit 后项目可编译运行
3. **先后端后前端**: 每个 API 先完成后端，再对接前端
4. **测试伴随**: 核心逻辑单测覆盖率 > 60%

---

## Phase 1: 模型管理核心（8 周）

### Week 1-2: 基础设施 + 数据库

#### T01: Maven 父工程 + 公共模块骨架
```
maidc-parent/pom.xml (dependencyManagement)
common/common-core/   ← R<T>, PageResult<T>, BusinessException, ErrorCode
common/common-redis/  ← RedisLockService, RedisUtil
common/common-minio/  ← MinioService
common/common-mq/     ← MaidcMessage, RabbitMQ 配置基类
common/common-log/    ← @OperLog 注解 + AOP
common/common-security/ ← JWT 工具, @CurrentUserId, SecurityUtils
common/common-jpa/    ← AuditorAware<String>, JsonNodeConverter, 软删除基类
```
**Commit**: `feat: 初始化 Maven 父工程和 common 公共模块`

#### T02: Docker Compose 基础设施
```
docker-compose.yml:
  PostgreSQL 15  (:5432)  ← 单库 maidc, 5 schema
  Redis 7        (:6379)
  MinIO          (:9000/:9001)
  RabbitMQ 3.12  (:5672/:15672)
  Nacos 2.x      (:8848)
```
**Commit**: `feat: 添加 Docker Compose 基础设施配置`

#### T03: 数据库 Schema + 全部 DDL（67 表）
```
docs/sql/01-schema.sql    ← CREATE SCHEMA cdr, rdr, model, system, audit
docs/sql/02-system.sql    ← 7 张 system 表 (s_user, s_role, s_permission, ...)
docs/sql/03-model.sql     ← 11 张 model 表 (m_model, m_model_version, ...)
docs/sql/04-cdr.sql       ← 28 张 cdr 表 (c_patient, c_encounter, ...)
docs/sql/05-rdr.sql       ← 19 张 rdr 表 (r_study_project, r_dataset, ...)
docs/sql/06-audit.sql     ← 3 张 audit 表 (a_audit_log, a_data_access_log, ...)
docs/sql/07-indexes.sql   ← 索引 + 推理日志分区表 + 初始数据
```
**Commit**: `feat: 添加完整数据库 DDL（67 张表，5 个 Schema）`

#### T04: Gateway 网关服务
```
maidc-gateway/ (:8080)
├── 路由规则（8 个微服务 Path 匹配）
├── 全局过滤器链: CorsFilter → TraceFilter → AuthFilter → RateLimiterFilter → RequestLogFilter
├── JWT 校验 + Redis 黑名单检查
├── 白名单: /api/v1/auth/login, /api/v1/auth/refresh
└── application.yml + bootstrap.yml (Nacos)
```
**Commit**: `feat: 实现 Spring Cloud Gateway 网关服务`

#### T05: Auth 认证服务
```
maidc-auth/ (:8081)
├── entity/   ← UserEntity, RoleEntity, PermissionEntity (schema=system)
├── repository/ ← UserRepository, RoleRepository
├── service/  ← AuthService, UserService, RoleService
│   ├── login (BCrypt 验证, 连续5次锁定)
│   ├── JWT 签发 (Access 2h + Refresh 7d)
│   ├── Token 缓存到 Redis
│   └── RBAC 权限查询
├── controller/ ← AuthController, UserController, RoleController
├── dto/vo    ← LoginDTO, TokenVO, UserVO, RoleVO
├── mapper/   ← AuthMapper (MapStruct)
├── config/   ← JpaConfig, SecurityConfig, RedisConfig
└── 初始数据: admin 用户 + 6 个内置角色 + 权限树
```
**Commit**: `feat: 实现 Auth 认证服务（JWT + RBAC + 用户管理）`

---

### Week 3-4: 模型管理后端 + 前端脚手架

#### T06: Model 模型 CRUD
```
maidc-model/ (:8083)
├── ModelEntity (schema=model, @Where软删除, @Auditing)
├── ModelRepository + ModelSpecification (动态查询)
├── ModelService: 注册/列表/详情/更新/删除
├── ModelController: RESTful API
├── DTO/VO: ModelCreateDTO, ModelUpdateDTO, ModelVO, ModelDetailVO
├── MapStruct: ModelMapper
├── Feign: AuthClient, DataClient
└── RabbitMQ: ModelRabbitMqConfig
```
**Commit**: `feat: 实现模型 CRUD 服务（注册/列表/详情/更新/删除）`

#### T07: Model 版本管理
```
├── ModelVersionEntity
├── VersionService: 创建版本(multipart上传到MinIO) / 列表 / 详情 / 对比
├── VersionController
├── MinIO 集成: bucket=maidc-models, path={org_id}/{model_code}/{version_no}/
└── SHA256 校验和计算
```
**Commit**: `feat: 实现模型版本管理（上传/列表/对比）`

#### T08: Model 评估 + 异步消息
```
├── EvaluationEntity
├── EvaluationService: 创建评估 / 查询结果 / 报告下载
├── RabbitMQ: 发送评估消息 → model.evaluation queue
├── EvaluationResultConsumer: 接收 aiworker 回调
└── EvaluationController
```
**Commit**: `feat: 实现模型评估服务（异步评估 + 结果回调）`

#### T09: Model 审批流程
```
├── ApprovalEntity
├── ApprovalService: 提交审批 / 多级审批(技术→临床→管理) / 审批详情
├── ApprovalController
└── 状态机: PENDING → APPROVED / REJECTED
```
**Commit**: `feat: 实现模型审批流程（多级审批 + 状态机）`

#### T10: 前端项目脚手架 + 登录页
```
maidc-portal/
├── Vite + Vue3 + TypeScript + Pinia + Ant Design Vue 4.x
├── 目录结构: api/ components/ views/ stores/ router/ hooks/ utils/ types/
├── Vite 配置: 代理到 Gateway :8080, 代码拆分
├── Axios 封装: 请求拦截(Token) / 响应拦截(401刷新) / TraceId
├── Pinia stores: authStore, permissionStore, uiStore
├── 路由: 静态路由 + 动态权限路由 + 路由守卫
├── layouts: BasicLayout(PageShell) + BlankLayout
└── 登录页 ← 对齐 .pen [nR4Uw] "01-Login Page"
    └── 用户名/密码表单 + JWT Token 存储
```
**Commit**: `feat: 初始化前端脚手架 + 登录页（对齐设计稿）`

#### T11: 前端公共组件库（44 组件）
```
components/
├── StatusBadge/    ← 22 种状态映射（对齐 .pen 44 个 StatusBadge 组件）
│   └── statusMap.ts: ModelStatus(4) + VersionStatus(6) + DeployStatus(5)
│                      + EvalStatus(4) + ApprovalStatus(3)
├── MetricCard/     ← 对齐 .pen [yjdAF]
├── JsonViewer/     ← 对齐 .pen [cQsH3]
├── FileUploader/   ← 对齐 .pen [qwZDx]
├── KeyValueEditor/ ← 对齐 .pen [iV7R2]
├── SearchForm/     ← 通用搜索表单
├── PageContainer/  ← 面包屑 + 标题
├── EmptyState/     ← 对齐 .pen [ps1ad]
├── MetricChart/    ← 对齐 .pen [D5W3y] (ECharts 封装)
├── PermissionWrapper/
├── useTable Hook
├── useModal Hook
├── usePermission Hook
└── Button/Primary, Button/Outline, Button/Danger ← 对齐 .pen
```
**Commit**: `feat: 实现前端公共组件库（对齐设计稿 44 组件）`

---

### Week 5-6: 部署/路由/监控 + 核心页面

#### T12: Model 部署管理
```
├── DeploymentEntity
├── DeploymentService: 创建部署 / 启动 / 停止 / 扩缩容 / 重启 / 状态查询
├── RabbitMQ: 部署消息 → model.deployment queue → aiworker 加载模型
├── DeploymentController
└── 资源配置: CPU/GPU/内存/副本数
```
**Commit**: `feat: 实现模型部署管理（创建/启停/扩缩容）`

#### T13: 流量路由 + 金丝雀发布
```
├── DeployRouteEntity (config JSONB)
├── RouteService: 创建路由 / 更新权重 / 指标对比
├── RouteController
├── Redis 缓存路由配置 maidc:route:{route_id}
└── 路由类型: CANARY / AB_TEST / WEIGHTED / MIRROR
```
**Commit**: `feat: 实现流量路由与金丝雀发布`

#### T14: 推理调用 + 监控指标 + 告警
```
├── InferenceLogEntity (按月分区)
├── InferenceService: 同步推理 → aiworker / 日志写入 / 指标采集
├── ModelMetricEntity
├── AlertRuleEntity + AlertRecordEntity
├── MonitoringController: 推理日志查询 / 运行指标查询
├── AlertController: 告警规则 CRUD / 告警确认 / 告警历史
└── Prometheus 指标暴露
```
**Commit**: `feat: 实现推理调用、监控指标与告警管理`

#### T15: AI Worker 服务 (Python)
```
maidc-aiworker/
├── FastAPI 入口: /v1/infer/{model_code}, /health, /v1/serving/load
├── Celery Worker: inference / evaluation / preprocessing 队列
├── 模型加载: PyTorch / ONNX / TensorFlow
├── 推理引擎适配: Triton / TorchServe / ONNX Runtime
├── 评估回调: PUT /api/v1/evaluations/{id}/result
├── 部署回调: PUT /api/v1/deployments/{id}/status
├── GPU 资源管理
└── pyproject.toml (Poetry)
```
**Commit**: `feat: 实现 AI Worker 服务（FastAPI + Celery）`

#### T16: 前端 - 仪表盘页
```
views/dashboard/
├── Overview.vue      ← 对齐 .pen [QptC0] "02-Dashboard Workbench"
│   ├── 模型总数/部署中/评估中/告警数
│   ├── 最近活动时间线
│   └── 快捷入口卡片
├── ModelDashboard.vue ← 模型监控看板（ECharts 指标图）
└── DataDashboard.vue  ← 数据监控看板
```
**Commit**: `feat: 实现仪表盘页面（对齐设计稿）`

#### T17: 前端 - 模型管理页面（14 页 + 弹窗）
```
views/model/
├── ModelList.vue         ← 对齐 .pen [Syc8a] "03-Model List"
│   └── Modal-Register    ← 对齐 .pen [cGet2] "Modal-Register Model"
│   └── Modal-Edit        ← 对齐 .pen [W7KS3] "Modal-Edit Model"
├── ModelDetail.vue       ← 对齐 .pen [7xGUL] "04-Model Details"
├── VersionList.vue       ← 对齐 .pen [ybnMi] "06-Version Management"
│   └── Modal-Upload      ← 对齐 .pen [VQF32] "Modal-Upload Version"
├── VersionCompare.vue    ← 对齐 .pen [WPf4T] "06a-Version Comparison"
├── EvalList.vue          ← 对齐 .pen [NyVaz] "07-Model Evaluation"
│   └── Modal-NewEval     ← 对齐 .pen [KsKs6] "Modal-New Evaluation"
├── EvalDetail.vue        ← 对齐 .pen [gBEhi] "07a-Evaluation Details"
│   └── ConfusionMatrix / RocCurve / MetricChart 组件
├── ApprovalList.vue      ← 对齐 .pen [DpQ4p] "08-Approval Process"
│   └── Modal-Approve     ← 对齐 .pen [Uo48v] "Modal-Approval Action"
├── ApprovalDetail.vue    ← 对齐 .pen [zBiWN] "08a-Approval Details"
├── DeploymentList.vue    ← 对齐 .pen [l2c3F] "05-Deployment Monitoring"
│   └── Modal-NewDeploy   ← 对齐 .pen [kH2Qv] "Modal-New Deployment"
├── DeploymentDetail.vue  ← 对齐 .pen [upipB] "05a-Deployment Details"
├── RouteConfig.vue       ← 对齐 .pen [eEgv3] "09-Traffic Routing"
│   └── Modal-CreateRoute ← 对齐 .pen [6OAKa] "Modal-Create Route"
├── InferenceLog.vue      ← 对齐 .pen [dqVsU] "Inference Logs"
└── AlertList.vue + AlertRuleList.vue ← 对齐 .pen [Skt7y] [znlwg]
```
**Commit**: `feat: 实现模型管理全部页面（14 页 + 弹窗，对齐设计稿）`

---

### Week 7-8: 系统设置 + 消息通知 + 联调

#### T18: 消息通知服务
```
maidc-msg/ (:8087)
├── MessageEntity, NotificationSettingEntity, MessageTemplateEntity
├── MessageService: 消息列表 / 标记已读 / 未读数
├── NotificationService: 通知设置 / 模板管理
├── RabbitMQ Consumer: 监听 model.alert / alert.notify 队列
├── WebSocket: 实时推送站内通知
└── 邮件发送: SMTP 集成
```
**Commit**: `feat: 实现消息通知服务（站内信 + WebSocket + 邮件）`

#### T19: 审计日志服务
```
maidc-audit/ (:8086)
├── AuditLogEntity, DataAccessLogEntity, SystemEventEntity (schema=audit)
├── AuditService: 操作审计 / 数据访问审计 / 系统事件 / 合规报表
├── AuditController
└── Feign: 被 model/data/auth 调用
```
**Commit**: `feat: 实现审计日志服务`

#### T20: 前端 - 系统设置页面（8 页 + 弹窗）
```
views/system/
├── UserList.vue          ← 对齐 .pen [fltE4] "20-User Management"
│   └── Modal-NewUser     ← 对齐 .pen [hD6y8]
│   └── Modal-EditUser    ← 对齐 .pen [ndESG]
│   └── Modal-ResetPwd    ← 对齐 .pen [vmeUl]
├── UserDetail.vue        ← 对齐 .pen [lNs6x] "20a-User Details"
├── RoleList.vue          ← 对齐 .pen [PWi45] "21-Role Management"
│   └── Modal-NewRole     ← 对齐 .pen [DXpFm]
├── RolePermission.vue    ← 对齐 .pen [0YAz3] "21a-Role Details"
├── PermissionTree.vue    ← 对齐 .pen [suAVK] "24-Permission Management"
├── OrgList.vue           ← 对齐 .pen [oSRs1] "23-Organization Management"
│   └── Modal-NewOrg      ← 对齐 .pen [cA3lv]
├── OrgDetail.vue         ← 对齐 .pen [EXQK4] "23a-Organization Details"
└── SystemConfig.vue      ← 对齐 .pen [nZr4C] "22-System Configuration"
```
**Commit**: `feat: 实现系统设置全部页面（对齐设计稿）`

#### T21: 前端 - 消息中心 + 审计日志 + 告警页面
```
views/message/
├── MessageList.vue       ← 对齐 .pen [gsUAE] "30-Message Center"
├── MessageDetail.vue     ← 对齐 .pen [pMRgm] "30a-Message Details"
├── NotificationSettings  ← 对齐 .pen [CD0G9] "31-Notification Settings"
└── TemplateList.vue      ← 对齐 .pen [kSlzk] "32-Template Management"

views/audit/
├── OperationLog.vue      ← 对齐 .pen [rL8XE] "15-Operation Audit"
├── OperationDetail.vue   ← 对齐 .pen [Xa61Z] "15a-Operation Details"
├── DataAccessLog.vue     ← 对齐 .pen [JyWj6] "15b-Data Access Audit"
├── SystemEvent.vue       ← 对齐 .pen [N1ww2] "15c-System Events"
└── ComplianceReport.vue  ← 对齐 .pen [dkYi4] "15d-Compliance Reports"

views/alert/
├── AlertList.vue         ← 对齐 .pen [Skt7y] "16-Alert Center"
└── AlertRuleList.vue     ← 告警规则配置
```
**Commit**: `feat: 实现消息中心/审计日志/告警页面（对齐设计稿）`

#### T22: 全链路联调 + Docker 部署脚本
```
├── 全链路测试: 登录 → 注册模型 → 上传版本 → 评估 → 审批 → 部署 → 推理
├── Docker Compose 一键启动全部服务
├── Nacos 配置导入
├── 数据库初始化脚本执行
├── MinIO Bucket 创建
└── README.md
```
**Commit**: `feat: 全链路联调 + Docker Compose 一键部署`

---

## Phase 2: 数据管理（8 周）

#### T23: Data 数据服务后端
```
maidc-data/ (:8082)
├── CDR 模块: PatientService, EncounterService, DiagnosisService, ...
├── RDR 模块: ProjectService, DatasetService, CohortService, ...
├── ETL 模块: EtlTaskService
├── 数据质量: QualityRuleService
├── 数据字典: DictService
├── 数据脱敏: DesensitizeService
└── Feign: AuthClient
```
**Commit**: `feat: 实现数据管理服务（CDR + RDR + ETL + 数据质量）`

#### T24: 前端 - CDR 数据管理页面（14 页 + 弹窗）
```
views/data-cdr/
├── PatientList.vue       ← 对齐 .pen [c2K7C]
├── PatientDetail.vue     ← 对齐 .pen [d3aVz] "Patient 360 View"
├── EncounterDetail.vue   ← 对齐 .pen [T33bQ] + 5 个子 Tab
├── DataSourceList.vue    ← 对齐 .pen [kveL5]
│   └── Modal-NewSource   ← 对齐 .pen [48Okh]
├── SyncTaskList.vue
├── QualityRuleList.vue   ← 对齐 .pen [snuTn]
│   └── Modal-NewRule     ← 对齐 .pen [a420R]
├── QualityResultList.vue
├── DesensitizeRule.vue   ← 对齐 .pen [7iqVm]
└── DictManage.vue        ← 对齐 .pen [az2Dc]
```
**Commit**: `feat: 实现 CDR 数据管理全部页面（对齐设计稿）`

#### T25: 前端 - RDR 科研管理页面（7 页 + 弹窗）
```
views/data-rdr/
├── ProjectList.vue       ← 对齐 .pen [skaqI]
│   └── Modal-NewProject  ← 对齐 .pen [d8ttO]
├── ProjectDetail.vue     ← 对齐 .pen [F6nGT]
│   └── Modal-Invite      ← 对齐 .pen [8ZeGy]
├── CohortList.vue
├── DatasetList.vue       ← 对齐 .pen [rX5KN]
│   └── Modal-NewDataset  ← 对齐 .pen [r1HaY]
├── DatasetDetail.vue     ← 对齐 .pen [aRSCp]
├── EtlTaskList.vue       ← 对齐 .pen [fstSG]
└── FeatureDictionary.vue ← 对齐 .pen [az2Dc]
```
**Commit**: `feat: 实现 RDR 科研管理全部页面（对齐设计稿）`

---

## Phase 3: 任务调度 + AI 集群（6 周）

#### T26: Task 任务调度服务
```
maidc-task/ (:8084)
├── XXL-JOB 集成
├── TaskService: 创建/更新/触发/暂停/执行记录
├── TaskController
└── 定时任务: CDR 增量同步 / 数据质量检测
```
**Commit**: `feat: 实现任务调度服务（XXL-JOB 集成）`

#### T27: 前端 - 任务调度页面
```
views/schedule/
├── TaskList.vue          ← 对齐 .pen [Ujek8]
│   └── Modal-NewTask     ← 对齐 .pen [DaZbW]
└── TaskDetail.vue        ← 对齐 .pen [4cH4A]
```
**Commit**: `feat: 实现任务调度页面（对齐设计稿）`

#### T28: Prometheus + Grafana 监控
```
├── Grafana Dashboard: 系统总览 / 模型推理 / 数据库 / 基础设施
├── Prometheus 指标采集配置
├── ELK 日志采集配置
└── Docker Compose 补充监控组件
```
**Commit**: `feat: 添加 Prometheus + Grafana 监控体系`

---

## Phase 4: 标注 + 审计 + 通知完善（6 周）

#### T29: Label 标注服务
```
maidc-label/ (:8085)
├── AnnotationTaskEntity (schema=rdr)
├── LabelTaskService: 创建/分配/AI预标注/统计
├── LabelTaskController
└── RabbitMQ: AI 预标注消息
```
**Commit**: `feat: 实现数据标注服务`

#### T30: 前端 - 标注管理页面（4 页）
```
views/label/
├── LabelTaskList.vue     ← 对齐 .pen [9EiHQ]
│   └── Modal-NewTask     ← 对齐 .pen [BRY7a]
├── LabelTaskDetail.vue   ← 对齐 .pen [4Eqcu]
├── LabelWorkspace.vue    ← 对齐 .pen [HgcK1] (影像标注)
└── LabelWorkspaceText    ← 对齐 .pen [Ns9np] (文本标注)
```
**Commit**: `feat: 实现标注管理页面（对齐设计稿）`

#### T31: 安全加固 + 性能优化
```
├── 等保三级合规检查
├── XSS/SQL 注入防护验证
├── 接口性能测试 (JMeter)
├── 缓存策略优化
└── 日志脱敏
```
**Commit**: `feat: 安全加固与性能优化`

---

## Phase 5: Portal 集成 + 上线（8 周）

#### T32: 全链路端到端集成
```
├── 数据接入 → 标注 → 训练 → 评估 → 审批 → 部署 → 推理 全流程打通
├── 集成测试用例
├── 用户验收测试 (UAT)
└── Bug 修复
```
**Commit**: `feat: 全链路端到端集成测试`

#### T33: 部署文档 + 用户手册
```
├── 运维部署手册
├── 用户操作手册
├── API 文档 (Swagger/OpenAPI)
└── 培训材料
```
**Commit**: `docs: 添加部署文档和用户手册`

---

## Commit 统计

| Phase | 任务数 | 预计 Commits | 周期 |
|-------|--------|-------------|------|
| Phase 1 | T01-T22 | 22 | 8 周 |
| Phase 2 | T23-T25 | 3 | 8 周 |
| Phase 3 | T26-T28 | 3 | 6 周 |
| Phase 4 | T29-T31 | 3 | 6 周 |
| Phase 5 | T32-T33 | 2 | 8 周 |
| **合计** | **33** | **33** | **36 周** |

---

## UI 对齐检查清单

每个页面对接设计稿时的验收标准：

1. **布局结构**: 侧边栏 + 顶栏 + 内容区 = PageShell 布局一致
2. **组件使用**: 必须使用 44 个公共组件，禁止自定义
3. **状态颜色**: StatusBadge 颜色严格对齐设计稿（22 种状态）
4. **间距规范**: 卡片间距 16px，表单项间距 8px
5. **弹窗规范**: Modal 宽度、按钮位置、表单布局对齐
6. **表格规范**: 列宽、操作列固定、分页器样式一致
7. **截图对比**: 每个页面完成后截图与 .pen 导出图做 diff

---

> **文档结束** - MAIDC 研发计划 v1.0
