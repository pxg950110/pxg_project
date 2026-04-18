# MAIDC 研发计划

> **版本**: v2.0
> **日期**: 2026-04-11
> **UI 基准**: `pencil-new.pen`（55 页 + 27 弹窗 + 44 组件）
> **规范基准**: `docs/dev/` 全部 6 份开发文档
> **提交策略**: 每个任务完成一次 git commit

---

## 规范来源追踪矩阵

每个任务标注其对应的开发文档来源：

| 来源文档 | 简写 | 覆盖范围 |
|----------|------|----------|
| `01-architecture.md` | [ARCH] | 微服务架构、通信机制、数据架构、网关、鉴权、可观测性 |
| `02-api-contract.md` | [API] | 103 个接口契约（请求/响应 JSON） |
| `03-frontend-guide.md` | [FE] | 前端脚手架、路由、组件、状态管理、API封装、页面模板 |
| `04-backend-guide.md` | [BE] | 工程结构、JPA分层、Entity/DTO/VO、MQ、异常处理、编码约定 |
| `05-flow-diagrams.md` | [FLOW] | 8 个交互流程图（F1-F8） |
| `06-dev-setup.md` | [SETUP] | Docker Compose、数据库初始化、MinIO、启动顺序 |

---

## Phase 1: 模型管理核心（8 周, T01-T26）

### Week 1-2: 工程骨架 + 基础设施 + 认证

#### T01: Maven 父工程 + 7 个公共模块
> [BE §1.1-1.3] [SETUP §5.1]

```
maidc-parent/
├── pom.xml                            ← Java 17, Spring Boot 3.2.x, Spring Cloud 2023.0.x
│                                      ← mapstruct 1.5.5.Final
├── common/
│   ├── common-core/                   ← R<T>, PageResult<T>, BusinessException, ErrorCode枚举
│   │   └── src/main/java/com/maidc/common/core/
│   │       ├── result/R.java          ← 统一响应体 code/message/data/traceId
│   │       ├── result/PageResult.java ← items/total/page/pageSize/totalPages
│   │       ├── exception/BusinessException.java
│   │       ├── exception/GlobalExceptionHandler.java ← @RestControllerAdvice
│   │       └── enums/ErrorCode.java   ← 400~5021 全部错误码
│   ├── common-redis/                  ← RedisLockService, RedisUtil
│   ├── common-minio/                  ← MinioService (upload/download/presignedUrl)
│   ├── common-mq/                     ← MaidcMessage基类, RabbitMQ配置基类
│   │   └── MaidcMessage.java          ← traceId/eventType/payload/timestamp/source
│   ├── common-log/                    ← @OperLog注解 + AOP切面
│   ├── common-security/               ← JWT工具, SecurityUtils, @CurrentUserId
│   └── common-jpa/                    ← JpaConfig(AuditorAware), JsonNodeConverter, 软删除基类
```
**Commit**: `feat: 初始化 Maven 父工程和 7 个 common 公共模块`

#### T02: Docker Compose 基础设施 + 数据库 DDL
> [SETUP §2-4] [ARCH §3]

```
docker/
├── docker-compose-infra.yml           ← PostgreSQL:5432, Redis:6379, MinIO:9000/9001,
│                                      ← RabbitMQ:5672/15672, Nacos:8848/9848
├── init-db/
│   ├── 01-schemas.sql                 ← CREATE SCHEMA system,cdr,rdr,model,audit + uuid-ossp + pgcrypto
│   ├── 02-system.sql                  ← 7张表 + admin用户 + 6角色 + 权限树 + 字典 + 配置
│   ├── 03-model.sql                   ← 11张表 (m_model/m_model_version/m_evaluation/
│   │                                    m_approval/m_deployment/m_deploy_route/
│   │                                    m_inference_log(分区)/m_model_metric/
│   │                                    m_alert_rule/m_alert_record + 索引)
│   ├── 04-cdr.sql                     ← 28张表 (c_patient→c_patient_bed)
│   ├── 05-rdr.sql                     ← 19张表 (r_study_project→r_data_quality_result)
│   └── 06-audit.sql                   ← 3张表 (a_audit_log/a_data_access_log/a_system_event)
```
**Commit**: `feat: 添加 Docker Compose + 完整 DDL（67表5Schema）`

#### T03: MinIO 初始化脚本 + Nacos 配置模板
> [SETUP §4-5.2] [ARCH §3.3]

```
docker/
├── init-minio.sh                      ← mc创建4个bucket: maidc-models/-dicom/-datasets/-docs
├── nacos-config/
│   ├── maidc-shared.yaml              ← 公共Redis/MinIO/RabbitMQ配置
│   ├── maidc-gateway-dev.yaml
│   ├── maidc-auth-dev.yaml
│   ├── maidc-model-dev.yaml
│   └── maidc-aiworker-dev.yaml
```
**Commit**: `feat: 添加 MinIO 初始化脚本 + Nacos 配置模板`

#### T04: Gateway 网关服务
> [ARCH §4] [API §1] [BE §1.2]

```
maidc-gateway/ (:8080)
├── config/RouteConfig.java            ← 8个微服务路由规则 [ARCH §4.1]
├── filter/
│   ├── CorsFilter.java                ← 跨域处理
│   ├── TraceFilter.java               ← 生成/透传 traceId → MDC
│   ├── AuthFilter.java                ← JWT校验 + Redis黑名单 [ARCH §5.1]
│   │                                  ← 注入 X-User-Id, X-Org-Id Header
│   │                                  ← 白名单: /api/v1/auth/login, /refresh, /captcha
│   ├── RateLimiterFilter.java         ← Sentinel 限流
│   └── RequestLogFilter.java          ← 请求日志（方法/URL/耗时）
└── bootstrap.yml                      ← Nacos注册
```
**Commit**: `feat: 实现 Gateway 网关（路由/鉴权/限流/追踪）`

#### T05: Auth 认证服务 — 全部 9 个 API
> [API §2] [BE §3] [FLOW F6] [ARCH §5]

```
maidc-auth/ (:8081)
├── entity/ (schema=system)
│   ├── UserEntity.java                ← s_user 16列 [BE §3.1]
│   ├── RoleEntity.java                ← s_role 7列
│   ├── PermissionEntity.java          ← s_permission 11列
│   ├── UserRoleEntity.java            ← s_user_role
│   └── RolePermissionEntity.java      ← s_role_permission
├── repository/
│   ├── UserRepository.java            ← [BE §3.3] findByUsername, existsByUsername
│   ├── RoleRepository.java
│   └── PermissionRepository.java
├── service/
│   ├── AuthService.java               ← [FLOW F6 §2-3] BCrypt验证 + 5次锁定
│   │                                  ← JWT签发 Access 2h + Refresh 7d
│   │                                  ← Redis缓存 maidc:auth:token:{userId}
│   ├── UserService.java               ← [API §2.4] CRUD + resetPassword
│   └── RoleService.java               ← [API §2.5] CRUD + permissions
├── controller/
│   ├── AuthController.java            ← POST /login, POST /refresh, POST /logout [API §2.1-2.3]
│   ├── UserController.java            ← GET/POST/PUT /users [API §2.4]
│   └── RoleController.java            ← GET/POST/PUT /roles, GET /permissions/tree [API §2.5]
├── dto/ ← LoginDTO, TokenDTO, UserCreateDTO, UserUpdateDTO, ResetPwdDTO
├── vo/  ← LoginVO(含user信息), UserVO, RoleVO, PermissionTreeVO
├── mapper/ ← AuthMapper (MapStruct) [BE §2.3]
└── config/ ← JpaConfig(AuditorAware<String>), SecurityConfig, RedisConfig
```
**Commit**: `feat: 实现 Auth 服务全部 9 个 API（JWT + RBAC + 用户管理）`

---

### Week 3-4: 模型管理后端（36 个 API 中的核心）

#### T06: Model 模型 CRUD — 5 个 API
> [API §3.1] [BE §3]

```
maidc-model/ (:8083) — 项目骨架 + 模型CRUD
├── ModelApplication.java
├── entity/ (schema=model)
│   └── ModelEntity.java               ← m_model 16列 [BE §3.1]
│                                      ← @Table(schema="model"), @Where, @SQLDelete
│                                      ← @DynamicUpdate, @AuditingEntityListener
│                                      ← @Convert(converter=JsonNodeConverter) for schemas
├── repository/
│   ├── ModelRepository.java           ← [BE §3.3] extends JpaRepository + JpaSpecificationExecutor
│   └── ModelSpecification.java        ← [BE §3.4] buildSearchSpec(orgId, keyword, modelType, status)
├── service/ModelService.java
│   └── impl/ModelServiceImpl.java     ← [BE §3.4] Specification动态查询 + PageResult.of()
├── controller/ModelController.java    ← [BE §2.4] @PreAuthorize, @Valid, R<VO>
│                                      ← POST /models, GET /models, GET /models/{id},
│                                      ← PUT /models/{id}, DELETE /models/{id}
├── dto/ ← ModelCreateDTO, ModelUpdateDTO (含 @NotBlank/@Size 校验) [BE §2.2]
├── vo/  ← ModelVO, ModelDetailVO [BE §2.2]
├── mapper/ModelMapper.java            ← [BE §2.3] @Mapper(componentModel="spring")
├── enums/ ← ModelStatus, VersionStatus, DeployStatus, EvalStatus, Framework, ModelType
├── config/ ← JpaConfig, RabbitMqConfig [BE §4.1]
├── feign/ ← AuthClient, DataClient    ← [ARCH §2.2]
└── mq/ ← ModelMessageProducer.java    ← [BE §4.2]
```
**Commit**: `feat: 实现 Model 服务骨架 + 模型 CRUD 5 个 API`

#### T07: Model 版本管理 — 4 个 API
> [API §3.2]

```
├── entity/ModelVersionEntity.java     ← m_model_version 17列
├── repository/VersionRepository.java
├── service/VersionService.java
│   ├── createVersion()                ← multipart上传到MinIO maidc-models bucket
│   │                                  ← SHA256 checksum计算
│   │                                  ← 路径: {org_id}/{model_code}/{version_no}/model.pt
│   ├── listVersions()
│   ├── getVersionDetail()
│   └── compareVersions()              ← [API §3.2] v1 vs v2 指标对比
├── controller/VersionController.java
│   ← POST /models/{id}/versions (multipart/form-data)
│   ← GET /models/{id}/versions
│   ← GET /models/{id}/versions/{vid}
│   ← GET /models/{id}/versions/compare?v1=&v2=
└── dto/vo ← VersionCreateDTO, VersionVO, VersionCompareVO
```
**Commit**: `feat: 实现模型版本管理 4 个 API（MinIO上传/对比）`

#### T08: Model 评估 + RabbitMQ 异步 — 3 个 API
> [API §3.3] [FLOW F1 §4] [BE §4] [ARCH §2.3]

```
├── entity/EvaluationEntity.java       ← m_evaluation 14列, metrics JSONB
├── repository/EvaluationRepository.java
├── service/EvaluationService.java
│   ├── createEvaluation()             ← [FLOW F1 §3] 创建任务 → 发MQ消息
│   ├── getEvaluation()                ← [API §3.3] 含metrics/confusion_matrix/report_url
│   └── getEvaluationReport()          ← PDF文件下载
├── controller/EvaluationController.java
│   ← POST /evaluations
│   ← GET /evaluations/{id}
│   ← GET /evaluations/{id}/report
├── mq/
│   ├── ModelMessageProducer.java      ← [BE §4.2] send to maidc.model exchange / evaluation key
│   └── EvaluationResultConsumer.java  ← [BE §4.3] consume from model.evaluation.result queue
│                                      ← [BE §4.4] 幂等性: 检查status!=RUNNING则跳过
└── config/ModelRabbitMqConfig.java    ← [BE §4.1] exchange/queue/binding + DLX
```
**Commit**: `feat: 实现模型评估 3 个 API（异步MQ + 结果回调）`

#### T09: Model 审批流程 — 3 个 API
> [API §3.4] [FLOW F1 §5-6]

```
├── entity/ApprovalEntity.java         ← m_approval 17列
├── repository/ApprovalRepository.java
├── service/ApprovalService.java
│   ├── submitApproval()               ← [FLOW F1 §5] 附带evidence_docs/risk_assessment
│   ├── reviewApproval()               ← [FLOW F1 §6] 多级审批: 技术→临床→管理
│   │                                  ← 任一 REJECTED 则流程终止
│   └── getApprovalDetail()
├── controller/ApprovalController.java
│   ← POST /approvals
│   ← PUT /approvals/{id}/review
│   ← GET /approvals/{id}
└── 状态机: PENDING → APPROVED / REJECTED
```
**Commit**: `feat: 实现审批流程 3 个 API（多级审批 + 状态机）`

#### T10: Model 部署 + 路由 + 推理 + 监控 + 告警 — 21 个 API
> [API §3.5-3.9] [FLOW F2, F3, F8]

```
├── entity/
│   ├── DeploymentEntity.java          ← m_deployment 16列, resource_config JSONB
│   ├── DeployRouteEntity.java         ← m_deploy_route 11列, config JSONB
│   ├── InferenceLogEntity.java        ← m_inference_log (按月分区) [BE §3.5]
│   ├── ModelMetricEntity.java         ← m_model_metric 9列
│   ├── AlertRuleEntity.java           ← m_alert_rule 12列
│   └── AlertRecordEntity.java         ← m_alert_record 11列
├── service/
│   ├── DeploymentService.java         ← [API §3.5] 创建/启动/停止/扩缩容/重启/状态
│   ├── RouteService.java              ← [API §3.6] [FLOW F3] 创建/更新路由 + 权重分配
│   │                                  ← Redis缓存 maidc:route:{id}
│   ├── InferenceService.java          ← [API §3.7] [FLOW F2] 同步推理→aiworker
│   │                                  ← 异步写入 m_inference_log + m_model_metric
│   │                                  ← 审计日志调用 audit-service
│   ├── MonitoringService.java         ← [API §3.8] 推理日志查询 + 运行指标查询
│   └── AlertService.java              ← [API §3.9] [FLOW F8] 告警规则CRUD + 确认 + 历史
├── controller/
│   ├── DeploymentController.java      ← 6 个 API [API §3.5]
│   ├── RouteController.java           ← 3 个 API [API §3.6]
│   ├── InferenceController.java       ← 1 个 API (POST /inference/{id}) [API §3.7]
│   ├── MonitoringController.java      ← 2 个 API [API §3.8]
│   └── AlertController.java           ← 6 个 API [API §3.9]
└── mq/ ← DeploymentMessageProducer (部署消息 → model.deployment queue)
```
**Commit**: `feat: 实现部署/路由/推理/监控/告警 21 个 API`

#### T11: AI Worker 服务 (Python)
> [API §9] [SETUP §7] [FLOW F1 §4, F2 §4]

```
maidc-aiworker/
├── app/
│   ├── main.py                        ← FastAPI 入口 (:8090)
│   ├── api/
│   │   ├── inference.py               ← POST /v1/infer/{model_code} [API §9.1]
│   │   ├── health.py                  ← GET /health [API §9.2]
│   │   └── serving.py                 ← POST /v1/serving/load [API §9.3]
│   ├── tasks/
│   │   ├── evaluation.py              ← Celery: 评估任务 (evaluation queue)
│   │   ├── inference_batch.py         ← Celery: 批量推理 (batch_inference queue)
│   │   └── preprocessing.py           ← Celery: 数据预处理 (preprocessing queue)
│   ├── models/                        ← 模型加载器 (PyTorch/ONNX/TF)
│   ├── core/
│   │   ├── config.py                  ← 环境变量读取
│   │   └── celery_app.py              ← Celery配置 (4个队列: inference/evaluation/preprocessing/batch_inference)
│   └── workers.py                     ← GET /v1/workers [API §9.4]
├── pyproject.toml                     ← Poetry: FastAPI 0.110+, Celery 5.3+, Pydantic 2.6+
└── Dockerfile
```
**Commit**: `feat: 实现 AI Worker 服务（FastAPI + Celery + 模型加载）`

---

### Week 5-6: 前端脚手架 + 公共组件 + 仪表盘

#### T12: 前端项目脚手架 + 登录页
> [FE §1] [SETUP §6]

```
maidc-portal/
├── Vite 5 + Vue 3.4 + TypeScript 5 + Pinia 2 + Ant Design Vue 4.x + ProComponents
├── src/
│   ├── main.ts                        ← 入口
│   ├── App.vue                        ← 根组件
│   ├── api/                           ← [FE §5.2] 按服务拆分
│   │   ├── auth.ts                    ← auth-service 接口 + TS类型
│   │   ├── model.ts                   ← model-service 接口 + TS类型 [FE §5.2 完整示例]
│   │   ├── data.ts / task.ts / label.ts / audit.ts / msg.ts
│   ├── utils/
│   │   ├── request.ts                 ← [FE §5.1] Axios实例 + 请求/响应拦截器
│   │   │                              ← Token注入 + TraceId + 401刷新 + 错误码处理
│   │   ├── auth.ts                    ← Token存取 (localStorage)
│   │   ├── date.ts                    ← ISO 8601格式化
│   │   └── validate.ts               ← 表单校验规则
│   ├── types/
│   │   ├── api.d.ts                   ← [FE §5.3] Response<T>, PageResult<T>, PageParams
│   │   ├── model.d.ts / data.d.ts / auth.d.ts / global.d.ts
│   ├── stores/                        ← [FE §4]
│   │   ├── auth.ts                    ← [FE §4.2] Token管理 + login/logout/getUserInfo
│   │   ├── permission.ts             ← [FE §4.3] 动态路由生成 + hasPermission()
│   │   └── ui.ts                      ← [FE §4.4] sidebarCollapsed/cachedViews/theme
│   ├── router/                        ← [FE §2]
│   │   ├── index.ts                   ← createRouter
│   │   ├── guards.ts                  ← [FE §2.4] beforeEach: Token检查→白名单→动态路由→权限
│   │   ├── constantRoutes.ts          ← [FE §2.2] /login, /403, /404, /500
│   │   └── asyncRoutes.ts             ← [FE §2.3] 8大模块全部路由定义
│   ├── hooks/
│   │   ├── useTable.ts                ← [FE §6.2] 表格分页查询Hook
│   │   ├── useModal.ts / usePermission.ts / useWebSocket.ts
│   ├── layouts/
│   │   ├── BasicLayout.vue            ← [FE §1.2] 主布局(侧边栏+顶栏+内容区)
│   │   ├── BlankLayout.vue            ← 空白布局(登录页)
│   │   └── PageShell.vue              ← [FE §2.5] keep-alive Tab缓存
│   └── views/login/LoginPage.vue      ← 对齐 .pen [nR4Uw] "01-Login Page"
├── vite.config.ts                     ← [FE §1.3] 代理/api→:8080 + 代码拆分
├── .env.development / .env.production ← [FE §1.4]
└── tsconfig.json
```
**Commit**: `feat: 初始化前端脚手架 + 登录页（对齐设计稿）`

#### T13: 前端公共组件库（44 组件 + 4 Hooks）
> [FE §3] [FE §6.2]

```
src/components/
├── StatusBadge/                       ← [FE §3.2] 22种状态映射表
│   ├── index.vue                      ← 对齐 .pen 22个StatusBadge组件
│   └── statusMap.ts                   ← ModelStatus(4)+VersionStatus(6)+DeployStatus(5)
│                                      ← EvalStatus(4)+ApprovalStatus(3)+AlertStatus(3)
├── MetricCard/                        ← 对齐 .pen [yjdAF] + props类型 [FE §3.4]
├── JsonViewer/                        ← 对齐 .pen [cQsH3]
├── FileUploader/                      ← 对齐 .pen [qwZDx] MinIO直传
├── KeyValueEditor/                    ← 对齐 .pen [iV7R2]
├── SchemaViewer/                      ← 输入/输出Schema展示
├── SearchForm/                        ← [FE §3.1] 通用搜索表单
├── PageContainer/                     ← 面包屑+标题+extra插槽
├── EmptyState/                        ← 对齐 .pen [ps1ad]
├── MetricChart/                       ← 对齐 .pen [D5W3y] ECharts封装
├── PermissionWrapper/                 ← [FE §3.1] v-permission指令
├── ModelSelect/ / DatasetSelect/ / UserSelect/  ← 业务选择器
├── VersionTag/ / ConfusionMatrix/ / RocCurve/   ← 评估组件
├── ApprovalTimeline/ / DeploymentStatus/        ← 流程组件
├── TrafficRuleEditor/ / ResourceConfigForm/      ← 配置组件
├── PatientInfoCard/ / DesensitizePreview/       ← 数据组件
├── ImagePreview/ / PdfViewer/ / CodeEditor/ / DiffViewer/  ← 通用工具
└── AuditDetailDrawer/ / CountDown/              ← 其他
```
**Commit**: `feat: 实现前端公共组件库 44 组件（对齐设计稿）`

#### T14: 前端 - 仪表盘 3 页
> [FE §2.3 Dashboard路由] 对齐 .pen [QptC0]

```
src/views/dashboard/
├── Overview.vue                       ← 对齐 .pen [QptC0] "02-Dashboard Workbench"
│   ├── MetricCard × 4 (模型总数/部署中/评估中/告警数)
│   ├── 最近活动时间线 (ApprovalTimeline)
│   └── 快捷入口卡片
├── ModelDashboard.vue                 ← 模型监控看板 (MetricChart ECharts)
└── DataDashboard.vue                  ← 数据监控看板
```
**Commit**: `feat: 实现仪表盘 3 页（对齐设计稿 [QptC0]）`

---

### Week 7-8: 模型管理全部页面 + 系统设置 + 联调

#### T15: 前端 - 模型管理 14 页 + 10 弹窗
> [API §3] [FE §6] 对齐 .pen 模型管理模块 14 屏 + 10 弹窗

```
src/views/model/
├── ModelList.vue                      ← 对齐 .pen [Syc8a] "03-Model List"
│   ├── SearchForm: keyword + model_type + status
│   ├── Table: 编码/名称/类型/框架/版本/状态/负责人/更新时间/操作
│   ├── Modal-Register                 ← 对齐 .pen [cGet2] "Modal-Register Model"
│   ├── Modal-Edit                     ← 对齐 .pen [W7KS3] "Modal-Edit Model"
│   └── Modal-DeleteConfirm            ← 对齐 .pen [TuuIC]
├── ModelDetail.vue                    ← 对齐 .pen [7xGUL] "04-Model Details"
│   ├── Descriptions: 基础信息卡片
│   └── Tabs: 版本/Schema/部署/统计
├── VersionList.vue                    ← 对齐 .pen [ybnMi] "06-Version Management"
│   ├── Modal-Upload                   ← 对齐 .pen [VQF32] "Modal-Upload Version" (FileUploader)
├── VersionCompare.vue                 ← 对齐 .pen [WPf4T] "06a-Version Comparison" (DiffViewer)
├── EvalList.vue                       ← 对齐 .pen [NyVaz] "07-Model Evaluation"
│   └── Modal-NewEval                  ← 对齐 .pen [KsKs6] "Modal-New Evaluation"
├── EvalDetail.vue                     ← 对齐 .pen [gBEhi] "07a-Evaluation Details"
│   ├── ConfusionMatrix / RocCurve / MetricChart
│   └── PdfViewer (报告下载)
├── ApprovalList.vue                   ← 对齐 .pen [DpQ4p] "08-Approval Process"
│   └── Modal-Approve                  ← 对齐 .pen [Uo48v] "Modal-Approval Action"
├── ApprovalDetail.vue                 ← 对齐 .pen [zBiWN] "08a-Approval Details"
│   └── ApprovalTimeline
├── DeploymentList.vue                 ← 对齐 .pen [l2c3F] "05-Deployment Monitoring"
│   └── Modal-NewDeploy                ← 对齐 .pen [kH2Qv] (ResourceConfigForm)
├── DeploymentDetail.vue               ← 对齐 .pen [upipB] "05a-Deployment Details"
│   └── DeploymentStatus + MetricChart
├── RouteConfig.vue                    ← 对齐 .pen [eEgv3] "09-Traffic Routing"
│   └── Modal-CreateRoute              ← 对齐 .pen [6OAKa] (TrafficRuleEditor)
├── InferenceLog.vue                   ← 对齐 .pen [dqVsU] "Inference Logs"
└── WorkerList.vue                     ← 对齐 .pen [Y7yfH] "AI Worker Cluster Management"
```
**Commit**: `feat: 实现模型管理全部 14 页 + 10 弹窗（对齐设计稿）`

#### T16: 消息通知服务 — 8 个 API
> [API §8] [ARCH §2.3 msg queues]

```
maidc-msg/ (:8087)
├── entity/ ← MessageEntity, NotificationSettingEntity, MessageTemplateEntity
├── service/
│   ├── MessageService.java            ← 消息列表/标记已读/全部已读/未读数
│   └── NotificationService.java       ← 通知设置CRUD/模板CRUD
├── controller/
│   ├── MessageController.java         ← GET/PUT /messages [API §8]
│   └── NotificationController.java    ← GET/PUT/POST /notifications [API §8]
├── mq/ ← AlertNotifyConsumer (监听 alert.notify 队列)
├── websocket/ ← WebSocket实时推送站内通知
└── email/ ← SMTP邮件发送
```
**Commit**: `feat: 实现消息通知服务 8 个 API（WebSocket + 邮件）`

#### T17: 审计日志服务 — 5 个 API
> [API §7] [FLOW F2 §7]

```
maidc-audit/ (:8086)
├── entity/ (schema=audit)
│   ├── AuditLogEntity.java            ← a_audit_log 19列
│   ├── DataAccessLogEntity.java       ← a_data_access_log 10列
│   └── SystemEventEntity.java         ← a_system_event 12列
├── service/AuditService.java          ← 操作审计/数据访问/系统事件/合规报表
├── controller/AuditController.java    ← GET /audit/operations, /data-access, /events, /reports [API §7]
└── feign/ ← 被其他服务远程调用
```
**Commit**: `feat: 实现审计日志服务 5 个 API`

#### T18: 前端 - 系统设置 8 页 + 7 弹窗
> [API §2.4-2.5] 对齐 .pen 系统设置模块

```
src/views/system/
├── UserList.vue                       ← 对齐 .pen [fltE4] "20-User Management"
│   ├── Modal-NewUser                  ← 对齐 .pen [hD6y8]
│   ├── Modal-EditUser                 ← 对齐 .pen [ndESG]
│   └── Modal-ResetPwd                 ← 对齐 .pen [vmeUl]
├── UserDetail.vue                     ← 对齐 .pen [lNs6x] "20a-User Details"
├── RoleList.vue                       ← 对齐 .pen [PWi45] "21-Role Management"
│   └── Modal-NewRole                  ← 对齐 .pen [DXpFm]
├── RolePermission.vue                 ← 对齐 .pen [0YAz3] "21a-Role Details" (权限树编辑)
├── PermissionTree.vue                 ← 对齐 .pen [suAVK] "24-Permission Management"
├── OrgList.vue                        ← 对齐 .pen [oSRs1] "23-Organization Management"
│   └── Modal-NewOrg                   ← 对齐 .pen [cA3lv]
├── OrgDetail.vue                      ← 对齐 .pen [EXQK4] "23a-Organization Details"
└── SystemConfig.vue                   ← 对齐 .pen [nZr4C] "22-System Configuration"
    └── Modal-NewConfig                ← 对齐 .pen [ICEQi]
```
**Commit**: `feat: 实现系统设置 8 页 + 7 弹窗（对齐设计稿）`

#### T19: 前端 - 消息中心 4 页 + 审计 5 页 + 告警 2 页
> [API §7,§8,§3.9] 对齐 .pen 消息/审计/告警模块

```
src/views/message/
├── MessageList.vue                    ← 对齐 .pen [gsUAE] "30-Message Center"
├── MessageDetail.vue                  ← 对齐 .pen [pMRgm] "30a-Message Details"
├── NotificationSettings.vue           ← 对齐 .pen [CD0G9] "31-Notification Settings"
└── TemplateList.vue                   ← 对齐 .pen [kSlzk] "32-Template Management"

src/views/audit/
├── OperationLog.vue                   ← 对齐 .pen [rL8XE] "15-Operation Audit"
├── OperationDetail.vue                ← 对齐 .pen [Xa61Z] "15a-Operation Details"
├── DataAccessLog.vue                  ← 对齐 .pen [JyWj6] "15b-Data Access Audit"
├── SystemEvent.vue                    ← 对齐 .pen [N1ww2] "15c-System Events"
└── ComplianceReport.vue               ← 对齐 .pen [dkYi4] "15d-Compliance Reports"

src/views/alert/
├── AlertList.vue                      ← 对齐 .pen [Skt7y] "16-Alert Center"
│   └── Modal-CreateAlertRule          ← 对齐 .pen [KMQTC]
└── AlertRuleList.vue                  ← 告警规则配置
```
**Commit**: `feat: 实现消息/审计/告警 11 页（对齐设计稿）`

#### T20: Task 任务调度服务 — 7 个 API
> [API §5]

```
maidc-task/ (:8084)
├── entity/ ← TaskEntity, TaskExecutionEntity
├── service/TaskService.java           ← 创建/更新/触发/暂停/执行记录
├── controller/TaskController.java     ← [API §5] 7个API
└── xxljob/ ← XXL-JOB 集成
```
**Commit**: `feat: 实现任务调度服务 7 个 API（XXL-JOB）`

#### T21: 前端 - 任务调度 2 页
> [API §5] 对齐 .pen 调度模块

```
src/views/schedule/
├── TaskList.vue                       ← 对齐 .pen [Ujek8] "13-Task Scheduling"
│   └── Modal-NewTask                  ← 对齐 .pen [DaZbW] "Modal-Create Scheduled Task"
└── TaskDetail.vue                     ← 对齐 .pen [4cH4A] "13a-Task Scheduling-Execution Details"
```
**Commit**: `feat: 实现任务调度 2 页（对齐设计稿）`

#### T22: 全链路联调 + 可观测性 + 部署脚本
> [FLOW F1-F8] [ARCH §6] [SETUP 全部]

```
├── 全链路测试脚本: 登录→注册模型→上传版本→评估→审批→部署→推理→告警
├── docker/docker-compose-full.yml     ← 全部服务一键启动
├── docker/init-nacos.sh              ← Nacos配置导入
├── monitoring/
│   ├── prometheus.yml                 ← 指标采集配置 [ARCH §6.2]
│   ├── grafana/dashboards/            ← 4个Dashboard [ARCH §6.3]
│   └── elk/                           ← ELK配置 [ARCH §6.4]
├── docs/sql/verify.sql                ← 数据完整性验证
└── README.md                          ← 项目说明 + 快速启动指南
```
**Commit**: `feat: 全链路联调 + 可观测性 + 一键部署`

---

## Phase 2: 数据管理（8 周, T23-T27）

#### T23: Data 数据服务后端 — 28 个 API
> [API §4] [ARCH §3.1 cdr+rdr schemas]

```
maidc-data/ (:8082)
├── CDR 模块 (28张表)
│   ├── PatientService                 ← [API §4.1] 患者/就诊/诊断/检验/用药/影像/体征/文书
│   ├── EncounterService               ← GET /cdr/encounters/{id} + 6个子资源
│   └── Patient360Service              ← GET /cdr/patients/{id}/360 聚合查询
├── RDR 模块 (19张表)
│   ├── ProjectService                 ← [API §4.2] 研究/成员/队列
│   ├── DatasetService                 ← 数据集/版本/访问日志
│   └── FeatureDictionaryService
├── ETL 模块
│   └── EtlTaskService                 ← [API §4.3] 创建/执行/暂停/日志
├── 数据质量
│   └── QualityRuleService             ← [API §4.4] 规则CRUD/执行/结果
├── 数据字典
│   └── DictService                    ← [API §4.5] 类型/项CRUD
├── 数据脱敏
│   └── DesensitizeService             ← [API §4.6] 规则/预览/导出
└── controller/ ← CdrController, RdrController, EtlController, QualityController, DictController, DesensitizeController
```
**Commit**: `feat: 实现数据管理服务 28 个 API（CDR+RDR+ETL+质量+字典+脱敏）`

#### T24: 前端 - CDR 数据管理 14 页 + 弹窗
> [API §4.1,§4.3-4.6] 对齐 .pen 数据管理模块

```
src/views/data-cdr/
├── PatientList.vue                    ← 对齐 .pen [c2K7C] "10-Patient List"
│   └── Modal-PatientForm              ← 手动录入
├── PatientDetail.vue                  ← 对齐 .pen [d3aVz] "Patient 360 View"
│   └── PatientInfoCard (脱敏展示)
├── EncounterDetail.vue                ← 对齐 .pen [T33bQ] + 5个子Tab:
│   ├── DiagnosisView.vue              ← 对齐 .pen [iMeB3]
│   ├── LabResultView.vue              ← 对齐 .pen [rihxc]
│   ├── ImagingView.vue                ← 对齐 .pen [DjcEf] (ImagePreview)
│   ├── MedicationView.vue             ← 对齐 .pen [YaM6W]
│   ├── VitalSignView.vue              ← 对齐 .pen [v2q18] (MetricChart)
│   └── ClinicalNoteView.vue           ← 对齐 .pen [y1TNN]
├── DataSourceList.vue                 ← 对齐 .pen [kveL5] "17-Data Source Management"
│   └── Modal-NewSource                ← 对齐 .pen [48Okh]
├── DataSourceDetail.vue               ← 对齐 .pen [ZqM8J]
├── SyncTaskList.vue
├── QualityRuleList.vue                ← 对齐 .pen [snuTn] "13-Data Quality"
│   └── Modal-NewRule                  ← 对齐 .pen [a420R]
├── QualityResultList.vue
├── DesensitizeRule.vue                ← 对齐 .pen [7iqVm]
│   └── Modal-MaskingPreview           ← 对齐 .pen [KIiKP]
└── DictManage.vue                     ← 对齐 .pen [az2Dc] + [BnANt] Data Dictionary Mapping
```
**Commit**: `feat: 实现 CDR 数据管理 14 页 + 弹窗（对齐设计稿）`

#### T25: 前端 - RDR 科研管理 7 页 + 弹窗
> [API §4.2] 对齐 .pen 科研管理模块

```
src/views/data-rdr/
├── ProjectList.vue                    ← 对齐 .pen [skaqI] "11-Research Projects"
│   └── Modal-NewProject               ← 对齐 .pen [d8ttO]
├── ProjectDetail.vue                  ← 对齐 .pen [F6nGT]
│   └── Modal-Invite                   ← 对齐 .pen [8ZeGy]
├── CohortList.vue                     ← 队列管理
├── DatasetList.vue                    ← 对齐 .pen [rX5KN] "12-Dataset Management"
│   └── Modal-NewDataset               ← 对齐 .pen [r1HaY]
├── DatasetDetail.vue                  ← 对齐 .pen [aRSCp] "12b-Dataset Details"
├── EtlTaskList.vue                    ← 对齐 .pen [fstSG] "14-ETL Tasks"
└── FeatureDictionary.vue              ← 对齐 .pen [az2Dc] "18-Feature Dictionary"
```
**Commit**: `feat: 实现 RDR 科研管理 7 页 + 弹窗（对齐设计稿）`

#### T26: 前端 - ETL 任务详情页
```
src/views/data-rdr/
└── EtlTaskDetail.vue                  ← ETL执行详情+日志
```
**Commit**: `feat: 实现 ETL 任务详情页`

---

## Phase 3: 标注服务 + 监控完善（6 周, T27-T29）

#### T27: Label 标注服务 — 6 个 API
> [API §6] [FLOW F7]

```
maidc-label/ (:8085)
├── entity/ ← LabelTaskEntity (schema=rdr)
├── service/LabelTaskService.java       ← [API §6] 创建/分配/AI预标注/统计
├── controller/LabelTaskController.java ← 6个API
└── mq/ ← AI预标注消息 → preprocessing queue
```
**Commit**: `feat: 实现标注服务 6 个 API（AI预标注 + 质量评估）`

#### T28: 前端 - 标注管理 4 页 + 3 弹窗
> [API §6] [FLOW F7] 对齐 .pen 标注模块

```
src/views/label/
├── LabelTaskList.vue                  ← 对齐 .pen [9EiHQ] "12-Annotation Management-Task List"
│   └── Modal-NewTask                  ← 对齐 .pen [BRY7a]
├── LabelTaskDetail.vue                ← 对齐 .pen [4Eqcu] "12a-Task Details" (LabelStats)
├── LabelWorkspace.vue                 ← 对齐 .pen [HgcK1] "25-Annotation Workbench-Image"
│   └── AnnotationCanvas (影像标注)
└── LabelWorkspaceText.vue             ← 对齐 .pen [Ns9np] "25a-Annotation Workbench-Text"
    └── Modal-AIPreAnnotate            ← 对齐 .pen [SGf9E]
    └── Modal-AnnotationReview         ← 对齐 .pen [i0zIK]
```
**Commit**: `feat: 实现标注管理 4 页 + 3 弹窗（对齐设计稿）`

#### T29: Prometheus + Grafana + ELK 监控体系
> [ARCH §6]

```
monitoring/
├── prometheus.yml                     ← 6个采集点 [ARCH §6.2]
├── grafana/
│   ├── provisionning/datasources/     ← Prometheus数据源
│   └── dashboards/
│       ├── system-overview.json       ← 全服务健康状态
│       ├── model-inference.json       ← 推理QPS/延迟/GPU [ARCH §6.3]
│       ├── database.json              ← 连接池/慢查询
│       └── infrastructure.json        ← CPU/内存/磁盘
├── docker/docker-compose-monitoring.yml ← Prometheus+Grafana+ELK
└── elk/
    ├── logstash.conf                  ← 日志采集管道
    └── kibana/                        ← 索引模板 maidc-log-{yyyy.MM.dd}
```
**Commit**: `feat: 添加 Prometheus + Grafana + ELK 监控体系`

---

## Phase 4: 安全加固 + 性能优化（4 周, T30-T31）

#### T30: 安全加固
> [BE §6.5] [ARCH §5]

```
├── XSS防护: DTO @Pattern校验 + 前端转义
├── SQL注入: JPA参数绑定审计
├── 字段加密: AesEncryptor AttributeConverter [BE §6.5]
├── BCrypt密码策略: 强制复杂度 + 过期提醒
├── 登录安全: 5次锁定 + IP白名单预留
├── 数据脱敏: 患者列表自动脱敏 [API §4.1]
└── 等保三级合规检查清单
```
**Commit**: `feat: 安全加固（XSS/SQL/加密/脱敏/等保）`

#### T31: 性能优化 + 缓存策略
> [ARCH §3.2]

```
├── Redis缓存策略落地 [ARCH §3.2 全部6种策略]
│   ├── Token Write-Through TTL=2h
│   ├── 权限 Cache-Aside TTL=30min
│   ├── 字典 Cache-Aside TTL=24h
│   ├── 模型 Cache-Aside TTL=10min
│   ├── 部署状态 Write-Behind TTL=30s
│   └── 分布式锁 TTL可配
├── 推理日志分区表维护 (pg_partman)
├── 慢SQL优化 (索引 + 执行计划分析)
├── 接口性能测试 (JMeter)
└── 前端打包优化 (gzip + 按需加载)
```
**Commit**: `feat: 性能优化（缓存/分区/索引/压测）`

---

## Phase 5: 集成上线（4 周, T32-T33）

#### T32: 端到端集成测试
> [FLOW F1-F8 全部8个流程]

```
├── F1: 模型全生命周期 E2E 测试
├── F2: 模型推理调用 E2E 测试
├── F3: 金丝雀发布 E2E 测试
├── F4: CDR 数据接入 E2E 测试
├── F5: ETL 数据转换 E2E 测试
├── F6: 用户认证鉴权 E2E 测试
├── F7: 数据标注 E2E 测试
├── F8: 告警触发通知 E2E 测试
└── Bug 修复
```
**Commit**: `feat: 全链路端到端集成测试（覆盖 8 个核心流程）`

#### T33: 文档 + 部署 + 交付
> [SETUP 全部]

```
├── README.md 更新（快速启动 + 架构图）
├── API 文档 (SpringDoc/Swagger)
├── 运维部署手册（Docker Compose → K8s）
├── 用户操作手册
└── 培训材料
```
**Commit**: `docs: 完善项目文档 + API文档 + 部署手册`

---

## Commit 统计

| Phase | 任务编号 | Commits | 周期 |
|-------|---------|---------|------|
| **Phase 1** | T01-T22 | 22 | 8 周 |
| **Phase 2** | T23-T26 | 4 | 8 周 |
| **Phase 3** | T27-T29 | 3 | 6 周 |
| **Phase 4** | T30-T31 | 2 | 4 周 |
| **Phase 5** | T32-T33 | 2 | 4 周 |
| **合计** | **T01-T33** | **33** | **30 周** |

## API 接口覆盖

| 服务 | 契约数 [API] | 任务覆盖 |
|------|-------------|----------|
| auth-service | 9 | T05 |
| model-service | 36 | T06-T10 |
| data-service | 28 | T23 |
| task-service | 7 | T20 |
| label-service | 6 | T27 |
| audit-service | 5 | T17 |
| msg-service | 8 | T16 |
| aiworker | 4 | T11 |
| **合计 103** | **103** | **全部覆盖** |

## UI 页面覆盖

| 模块 | 页面数 | 弹窗数 | 任务覆盖 |
|------|--------|--------|----------|
| 登录 | 1 | 0 | T12 |
| 仪表盘 | 3 | 0 | T14 |
| 模型管理 | 14 | 10 | T15 |
| 系统设置 | 8 | 7 | T18 |
| 消息中心 | 4 | 0 | T19 |
| 审计日志 | 5 | 0 | T19 |
| 告警中心 | 2 | 1 | T19 |
| CDR数据 | 14 | 4 | T24 |
| RDR科研 | 7 | 3 | T25+T26 |
| 标注管理 | 4 | 3 | T28 |
| 任务调度 | 2 | 1 | T21 |
| **合计** | **~64** | **~29** | **全部覆盖** |

## 完成状态

> **更新时间**: 2026-04-11
> **全部 33 个任务已完成**

| Phase | 任务 | 状态 | 完成说明 |
|-------|------|------|---------|
| Phase 1 | T01-T22 | ✅ 全部完成 | 后端8服务+前端核心页面+监控 |
| Phase 2 | T23-T26 | ✅ 全部完成 | CDR 14页+RDR 7页+ETL详情 |
| Phase 3 | T27-T29 | ✅ 全部完成 | 标注4页+Prometheus+Grafana |
| Phase 4 | T30-T31 | ✅ 全部完成 | 安全加固+性能优化 |
| Phase 5 | T32-T33 | ✅ 全部完成 | E2E测试+文档 |

---

## 流程覆盖 [FLOW]

| 流程 | 任务 |
|------|------|
| F1 模型全生命周期 | T06→T07→T08→T09→T10→T15→T22 |
| F2 模型推理调用 | T10→T11→T22 |
| F3 金丝雀发布 | T10→T15→T22 |
| F4 CDR数据接入 | T23→T24 |
| F5 ETL数据转换 | T23→T25 |
| F6 用户认证鉴权 | T04→T05→T12 |
| F7 数据标注 | T27→T28 |
| F8 告警触发通知 | T10→T16→T19→T22 |

---

## UI 对齐检查清单

每个前端任务提交前的验收标准：

1. **布局**: PageShell 侧边栏+顶栏+内容区 = .pen 导出截图一致
2. **组件**: 必须使用 StatusBadge/MetricCard/FileUploader 等 44 公共组件
3. **状态颜色**: 22 种 StatusBadge 颜色严格对齐 statusMap.ts
4. **间距**: 卡片 16px, 表单 8px, 表格行高 54px
5. **弹窗**: Modal 宽度/按钮位置/表单布局对齐 .pen 弹窗节点
6. **表格**: 列宽/操作列 fixed right/分页器样式/空状态
7. **截图**: 每页完成后截图与 .pen 导出图做 diff

---

> **文档结束** - MAIDC 研发计划 v2.0
