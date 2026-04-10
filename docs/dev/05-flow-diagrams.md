# MAIDC 交互流程图集

> **版本**: v1.0
> **日期**: 2026-04-10
> **项目**: MAIDC - Medical AI Data Center 医疗AI数据中心
> **技术栈**: Spring Cloud Alibaba + Python AI Worker + PostgreSQL + RabbitMQ + MinIO + Redis

---

## 目录

1. [F1: 模型全生命周期流程](#f1-模型全生命周期流程) ★最重要
2. [F2: 模型推理调用流程](#f2-模型推理调用流程)
3. [F3: 金丝雀发布与流量路由](#f3-金丝雀发布与流量路由)
4. [F4: CDR 数据接入流程](#f4-cdr-数据接入流程)
5. [F5: ETL 数据转换管线](#f5-etl-数据转换管线)
6. [F6: 用户认证与鉴权流程](#f6-用户认证与鉴权流程)
7. [F7: 数据标注工作流](#f7-数据标注工作流)
8. [F8: 告警触发与通知流程](#f8-告警触发与通知流程)

---

## F1: 模型全生命周期流程

### 业务背景

模型全生命周期管理是 MAIDC 平台最核心的业务流程，覆盖从模型注册、版本上传、评估验证、多级审批到上线部署的完整链路。该流程涉及前端 Portal、API 网关、模型管理服务、对象存储、AI Worker 集群和消息通知服务六个参与方，通过 RabbitMQ 实现异步解耦，通过多级审批保障模型上线的安全性与合规性。

### 时序图

```plantuml
@startuml
title F1: 模型全生命周期流程
autonumber

actor "AI工程师" as engineer
participant "Portal\n(Vue3)" as portal
participant "API Gateway\n(SCG)" as gateway
participant "model-service\n(Java)" as model
participant "MinIO\n(对象存储)" as minio
participant "RabbitMQ" as mq
participant "aiworker\n(Python)" as aiworker
participant "msg-service\n(Java)" as msg

== 1. 模型注册 ==
engineer -> portal : 填写模型信息\n(model_code, model_type,\nframework, task_type)
portal -> gateway : POST /api/v1/models\n(model_code, model_type,\nframework, input_schema,\noutput_schema)
gateway -> model : 路由转发
model -> model : 校验参数\n检查 model_code 唯一性
model --> gateway : 201 Created\n(id, status=DRAFT)
gateway --> portal : 返回模型ID
portal --> engineer : 模型注册成功

== 2. 版本上传 ==
engineer -> portal : 上传模型文件\n(.pt/.onnx/.pkl)
portal -> gateway : POST /api/v1/models/id/versions\nmultipart/form-data
gateway -> model : 路由转发
model -> minio : 上传模型文件\nbucket: maidc-models
minio --> model : 返回 storage_path
model -> model : 计算 SHA256 校验和\n写入 m_model_version\nstatus=CREATED
model -> msg : 发送通知\n"版本上传完成"
msg --> engineer : 站内信/邮件通知
model --> gateway : 201 Created\n(version_id, version_no)
gateway --> portal : 版本创建成功
portal --> engineer : 版本上传完成

== 3. 创建评估任务 ==
engineer -> portal : 选择评估数据集\n配置评估参数
portal -> gateway : POST /api/v1/evaluations\n(model_version_id,\ndataset_id, eval_type,\nmetrics_config)
gateway -> model : 路由转发
model -> model : 创建 m_evaluation\n关联 r_dataset\nstatus=PENDING
model -> mq : 发送评估消息\nexchange: model.evaluation\nrouting_key: eval.start
model --> gateway : 201 Created\n(evaluation_id)
gateway --> portal : 评估任务已创建

== 4. 异步执行评估 ==
mq -> aiworker : 消费评估消息\n(Celery Worker)
aiworker -> minio : 下载模型文件
minio --> aiworker : 模型二进制
aiworker -> aiworker : 加载模型\n加载评估数据集\n执行推理评估
aiworker -> aiworker : 计算 AUC/F1/IoU\n等评估指标
aiworker -> gateway : PUT /api/v1/evaluations/id/result\n(metrics, confusion_matrix,\nroc_data, status=COMPLETED)
gateway -> model : 写入评估结果
model -> model : 更新 m_evaluation\nmetrics JSONB
model -> msg : 发送通知\n"评估完成: AUC=0.92"
msg --> engineer : 站内信/邮件通知

== 5. 提交审批 ==
engineer -> portal : 提交上线审批\n附带评审材料
portal -> gateway : POST /api/v1/approvals\n(model_version_id,\napproval_type=DEPLOY,\nevidence_docs, risk_assessment,\nclinical_validation)
gateway -> model : 路由转发
model -> model : 创建 m_approval\nstatus=PENDING
model -> msg : 发送通知\n"待审批: 技术评审"
msg --> engineer : 通知审批人

== 6. 多级审批 ==
note over model
  审批链路:
  技术评审 -> 临床评审 -> 管理审批
  任一环节 REJECTED 则流程终止
end note

model <- gateway : PUT /api/v1/approvals/id/review\n(result=APPROVED,\nresult_comment,\nreviewer_id)
model -> model : 技术评审通过
model -> msg : 通知下一级审批人
model <- gateway : PUT /api/v1/approvals/id/review\n(result=APPROVED)
model -> model : 临床评审通过
model <- gateway : PUT /api/v1/approvals/id/review\n(result=APPROVED)
model -> model : 管理审批通过\nm_approval.result=APPROVED
model -> msg : 发送通知\n"审批全部通过"
msg --> engineer : 审批通过通知

== 7. 创建部署 ==
engineer -> portal : 配置部署参数\n(CPU/GPU/内存/副本/框架)
portal -> gateway : POST /api/v1/deployments\n(model_version_id,\ndeployment_type=ONLINE,\nserving_framework=TRITON,\nresource_config, auto_scale)
gateway -> model : 路由转发
model -> model : 创建 m_deployment\nstatus=CREATING
model -> mq : 发送部署消息\nexchange: model.deployment\nrouting_key: deploy.create
mq -> aiworker : 消费部署消息\n加载模型到推理引擎
aiworker -> aiworker : 初始化 Triton/TorchServe\n配置 GPU 资源\n健康检查
aiworker -> gateway : PUT /api/v1/deployments/id/status\n(status=RUNNING,\nendpoint_url, health_check_url)
gateway -> model : 更新部署状态
model -> msg : 发送通知\n"部署上线成功"
msg --> engineer : 部署运行通知

@enduml
```

### 关键说明

| 步骤 | API 接口 | 数据库表 | 说明 |
|------|---------|----------|------|
| 模型注册 | `POST /api/v1/models` | `model.m_model` | 创建模型基础信息，初始状态为 DRAFT |
| 版本上传 | `POST /api/v1/models/{id}/versions` | `model.m_model_version` | multipart 上传模型文件到 MinIO，计算 SHA256 校验 |
| 创建评估 | `POST /api/v1/evaluations` | `model.m_evaluation` | 关联评估数据集（`rdr.r_dataset`），状态 PENDING |
| 异步评估 | RabbitMQ `model.evaluation.queue` | `model.m_evaluation` | model-service 发消息，aiworker Celery Worker 异步执行评估 |
| 评估回调 | `PUT /api/v1/evaluations/{id}/result` | `model.m_evaluation` | aiworker 通过 HTTP 回调写入评估指标（AUC/F1/IoU 等） |
| 提交审批 | `POST /api/v1/approvals` | `model.m_approval` | 附带评审材料、风险评估、临床验证说明 |
| 多级审批 | `PUT /api/v1/approvals/{id}/review` | `model.m_approval` | 技术评审 -> 临床评审 -> 管理审批，任一环节拒绝则流程终止 |
| 创建部署 | `POST /api/v1/deployments` | `model.m_deployment` | 配置资源（CPU/GPU/内存/副本），选择推理框架（Triton/TorchServe） |
| 全程通知 | - | - | model-service 在每个关键节点通过 msg-service 发送通知 |

**状态机**:
- 模型: `DRAFT -> REGISTERED -> PUBLISHED -> DEPRECATED`
- 版本: `CREATED -> TRAINING -> EVALUATING -> APPROVED -> DEPLOYED -> DEPRECATED`
- 审批: `PENDING -> APPROVED / REJECTED`
- 部署: `CREATING -> RUNNING -> STOPPING -> STOPPED / FAILED`

---

## F2: 模型推理调用流程

### 业务背景

模型推理是模型上线后的核心业务动作，由临床系统（HIS）或前端 Portal 发起，经网关路由至 model-service，再通过 FastAPI 同步调用 aiworker 执行推理。推理全程记录推理日志（`m_inference_log`）、采集运行指标、写入审计日志，确保临床 AI 辅助诊断过程可追溯、可审计。

### 时序图

```plantuml
@startuml
title F2: 模型推理调用流程
autonumber

actor "临床医生/\nHIS系统" as caller
participant "API Gateway\n(SCG)" as gateway
participant "model-service\n(Java)" as model
participant "Redis\n(缓存)" as redis
participant "aiworker\n(FastAPI)" as aiworker
participant "MinIO\n(对象存储)" as minio
participant "audit-service\n(Java)" as audit
participant "Prometheus\n(监控)" as prom

== 1. 推理请求入口 ==
caller -> gateway : POST /api/v1/inference/deployment_id\n(request_id, patient_id,\nencounter_id, input)

== 2. Gateway 鉴权与限流 ==
gateway -> gateway : 解析 JWT Token\n验证签名和过期时间
gateway -> redis : 检查 Token 黑名单\nmaidc:token:blacklist
redis --> gateway : Token 有效
gateway -> gateway : 检查请求限流\nRate Limiter

== 3. 路由匹配 ==
gateway -> model : 转发推理请求
model -> model : 查询 m_deploy_route\n根据金丝雀/加权/镜像规则\n选择目标 deployment
model -> redis : 查询 deployment 缓存\nmaidc:deploy:id
redis --> model : deployment 信息\n(endpoint_url, model_version,\nstatus=RUNNING)
model -> model : 校验 deployment 状态\n确认 RUNNING

== 4. 同步推理调用 ==
model -> aiworker : HTTP POST\n/v1/infer/model_code\n(input, parameters)
aiworker -> minio : 获取推理输入文件\n(如 DICOM 影像)
minio --> aiworker : 返回文件数据
aiworker -> aiworker : 数据预处理\n(归一化/Resize/格式转换)
aiworker -> aiworker : 执行模型推理\n(PyTorch/ONNX/TensorRT)
aiworker -> aiworker : 后处理\n(NMS/置信度过滤/结果解析)
aiworker --> model : 200 OK\n(results, confidence,\nlatency_ms)

== 5. 记录推理日志 ==
model -> model : 异步写入 m_inference_log\n(request_id, patient_id,\ninput_summary, output_result,\nconfidence, latency_ms, status)
note over model
  m_inference_log 按月分区:
  PARTITION BY RANGE (created_at)
  单月百万级记录
end note

== 6. 采集运行指标 ==
model -> prom : 暴露 Prometheus 指标\ninference_latency_ms\ninference_total\ninference_errors_total
model -> model : 聚合写入 m_model_metric\n(QPS, P50, P99, GPU利用率)

== 7. 写入审计日志 ==
model -> audit : 异步调用 audit-service\n(患者数据访问记录)
audit -> audit : 写入 a_audit_log\n(trace_id, user_id,\noperation=INFERENCE,\nresource_type, duration_ms)
audit -> audit : 写入 a_data_access_log\n(patient_id, access_type=QUERY,\ndata_domain=MODEL)

== 8. 返回推理结果 ==
model --> gateway : 200 OK\n(request_id, results,\nconfidence, latency_ms,\nmodel_version)
gateway --> caller : 推理结果响应

@enduml
```

### 关键说明

| 步骤 | API 接口 | 数据库表 | 说明 |
|------|---------|----------|------|
| 推理请求 | `POST /api/v1/inference/{deployment_id}` | - | 支持来自 HIS/PACS 等临床系统的调用 |
| 路由匹配 | 内部查询 `m_deploy_route` | `model.m_deploy_route` | 根据金丝雀/加权/镜像规则路由到目标 deployment |
| 同步推理 | aiworker FastAPI `/v1/infer/{model_code}` | - | HTTP 同步调用，延迟通常在 100-500ms |
| 推理日志 | 内部写入 | `model.m_inference_log` | 按月分区存储，含输入摘要、输出结果、延迟、状态 |
| 指标采集 | 内部聚合 | `model.m_model_metric` | QPS、延迟 P50/P99、GPU 利用率等 |
| 审计日志 | 异步调用 audit-service | `audit.a_audit_log` + `audit.a_data_access_log` | 全链路追踪，含患者数据访问记录 |

**性能要求**: 单次同步推理延迟 P99 < 500ms，推理日志异步写入不阻塞主流程。

---

## F3: 金丝雀发布与流量路由

### 业务背景

金丝雀发布（Canary Release）是模型平滑升级的关键策略。AI 工程师创建新版本部署后，通过部署路由（`m_deploy_route`）将少量流量（如 10%）导向新版本，在监控指标对比验证通过后，逐步或全量切换到新版本。该机制支持手动提升和自动提升两种模式，避免新模型版本上线对临床业务造成影响。

### 时序图

```plantuml
@startuml
title F3: 金丝雀发布与流量路由
autonumber

actor "AI工程师" as engineer
participant "Portal\n(Vue3)" as portal
participant "API Gateway\n(SCG)" as gateway
participant "model-service\n(Java)" as model
participant "Redis\n(缓存)" as redis
participant "aiworker\n(旧版本)" as worker_old
participant "aiworker\n(新版本)" as worker_new
participant "Prometheus\n(监控)" as prom

box "现有部署" #LightBlue
    participant worker_old
end box

box "金丝雀部署" #LightGreen
    participant worker_new
end box

== 1. 创建金丝雀部署 ==
engineer -> portal : 创建新版本部署\n(v2.0.0, TRITON,\nresource_config)
portal -> gateway : POST /api/v1/deployments\n(model_version_id=v2.0.0,\ndeployment_type=ONLINE)
gateway -> model : 路由转发
model -> model : 创建 m_deployment\nstatus=CREATING
model -> worker_new : 初始化推理服务\n加载 v2.0.0 模型
worker_new --> model : status=RUNNING\nendpoint_url_ready

== 2. 创建金丝雀路由 ==
engineer -> portal : 配置金丝雀规则\n(新版本10%, 旧版本90%)
portal -> gateway : POST /api/v1/routes\n(route_type=CANARY,\ndefault_deployment_id=旧,\nconfig:\n[(旧, weight=90),\n(新, weight=10)])
gateway -> model : 路由转发
model -> model : 创建 m_deploy_route\n写入路由配置 JSONB
model -> redis : 缓存路由配置\nmaidc:route:route_id
model --> gateway : 201 Created\n(route_id)
gateway --> portal : 路由创建成功
portal --> engineer : 金丝雀发布已启动\n流量: 旧90% / 新10%

== 3. 金丝雀流量分发 ==
note over model
  每次推理请求到达时:
  1. 查询 m_deploy_route 缓存
  2. 按权重随机分配
  3. 记录命中的 deployment_id
end note

loop 每次推理请求
    caller -> gateway : POST /api/v1/inference/model_code
    gateway -> model : 路由转发
    model -> redis : 获取路由配置\nmaidc:route:route_id
    redis --> model : 路由规则\n(canary_config)
    model -> model : 按权重随机分配\n90% -> 旧, 10% -> 新
    alt 命中旧版本 (90%)
        model -> worker_old : POST /v1/infer/model_code
        worker_old --> model : 推理结果 (v1.x)
    else 命中新版本 (10%)
        model -> worker_new : POST /v1/infer/model_code
        worker_new --> model : 推理结果 (v2.0.0)
    end
    model -> model : 记录推理日志\n标注 deployment_id
end

== 4. 指标对比分析 ==
model -> prom : 查询新旧版本指标
prom --> model : 旧版本: P99=280ms, 错误率=0.2%\n新版本: P99=245ms, 错误率=0.1%
engineer -> portal : 查看对比报告
portal -> gateway : GET /api/v1/monitoring/routes/id/comparison
gateway -> model : 路由转发
model --> gateway : 新旧版本指标对比\n(延迟, 错误率, 准确率)
gateway --> portal : 对比数据
portal --> engineer : 展示对比报告\n新版本表现优于旧版本

== 5a. 手动提升流量 ==
engineer -> portal : 手动提升至 50%
portal -> gateway : PUT /api/v1/routes/id\n(config:\n[(旧, weight=50),\n(新, weight=50)])
gateway -> model : 更新路由配置
model -> model : 更新 m_deploy_route.config
model -> redis : 刷新路由缓存
model --> portal : 更新成功

== 5b. 自动提升模式 ==
note over model
  当 auto_promote=true 时:
  定时检查新版本指标是否
  达到 success_threshold
end note
model -> model : 定时检查\n新版本 success_rate >= 0.95?
model -> model : 自动提升权重\n10% -> 25% -> 50%\n-> 75% -> 100%
model -> redis : 更新路由缓存

== 6. 全量切换完成 ==
engineer -> portal : 全量切换到新版本
portal -> gateway : PUT /api/v1/routes/id\n(config:\n[(新, weight=100)])
gateway -> model : 更新路由配置
model -> model : 更新 default_deployment_id\n指向新版本
model -> redis : 刷新路由缓存
model --> portal : 切换完成
portal --> engineer : 金丝雀发布完成\n所有流量已切换到 v2.0.0

@enduml
```

### 关键说明

| 步骤 | API 接口 | 数据库表 | 说明 |
|------|---------|----------|------|
| 创建路由 | `POST /api/v1/routes` | `model.m_deploy_route` | 配置路由类型（CANARY/AB_TEST/WEIGHTED/MIRROR）和流量分配 |
| 流量分配 | 内部逻辑 | `model.m_deploy_route.config` | 按权重随机分配，记录每次请求命中的 deployment |
| 指标对比 | `GET /api/v1/monitoring/routes/{id}/comparison` | `model.m_model_metric` | 对比新旧版本的延迟、错误率、准确率等指标 |
| 手动提升 | `PUT /api/v1/routes/{id}` | `model.m_deploy_route` | 工程师手动调整权重比例，逐步切换 |
| 自动提升 | 内部定时检查 | `model.m_deploy_route.traffic_rules` | 根据 `success_threshold` 自动逐步提升新版本流量 |

**路由类型说明**:
- **CANARY**: 金丝雀发布，按比例分发流量到新旧版本
- **AB_TEST**: A/B 测试，按用户/请求特征分流
- **WEIGHTED**: 加权路由，多个 deployment 按权重分配
- **MIRROR**: 流量镜像，主流量走生产，镜像一份到新版本（不影响线上）

---

## F4: CDR 数据接入流程

### 业务背景

CDR（Clinical Data Repository）数据接入流程实现从外部医院信息系统（HIS/PACS/LIS）到 MAIDC 临床数据仓库的标准化数据同步。流程涵盖数据源注册、增量同步触发、数据字典映射（ICD-10/LOINC/SNOMED CT）、数据质量校验、写入 CDR Schema 以及 Redis 缓存更新，确保临床数据的完整性和标准化。

### 流程图

```mermaid
flowchart TD
    Start([开始]) --> RegSource[注册数据源\nPOST /api/v1/datasources\n表: cdr.c_org]
    RegSource --> ConfigConn[配置连接参数\n{host, port, db_type, auth}\n支持: HIS / PACS / LIS]

    ConfigConn --> TestConn{测试连接\n是否成功?}
    TestConn -->|成功| TrigSync[触发增量同步\n定时(XXL-JOB) / 手动触发]
    TestConn -->|失败| FixConn[排查网络/权限问题]
    FixConn --> ConfigConn

    TrigSync --> Extract[增量数据抽取\n读取 source_system + source_id\n比对 last_sync_time]
    Extract --> DictMap[数据字典映射\nICD-10 诊断编码\nLOINC 检验项目编码\nSNOMED CT 临床术语]

    DictMap --> QualityCheck{数据质量校验\nr_data_quality_rule}
    QualityCheck -->|通过| Transform[数据转换\n脱敏处理(姓名/身份证/电话)\n格式标准化(日期/编码/单位)]
    QualityCheck -->|不通过| Quarantine[隔离问题数据\n记录错误明细\n通知数据管理员]

    Transform --> WriteCDR[写入 CDR Schema\n表: c_patient / c_encounter\n/ c_diagnosis / c_lab_test\n/ c_imaging_exam 等]
    WriteCDR --> UpdateCache[更新 Redis 缓存\n患者基本信息缓存\n科室/医生字典缓存]
    UpdateCache --> SyncLog[记录同步日志\n{records_read, records_written,\nrecords_error, duration}]

    SyncLog --> End([结束])

    Quarantine --> ManualFix[人工修正数据]
    ManualFix --> TrigSync

    style Start fill:#4CAF50,color:#fff
    style End fill:#4CAF50,color:#fff
    style QualityCheck fill:#FF9800,color:#fff
    style Quarantine fill:#F44336,color:#fff
    style WriteCDR fill:#2196F3,color:#fff
    style UpdateCache fill:#9C27B0,color:#fff
```

### 关键说明

| 步骤 | API 接口 | 数据库表 | 说明 |
|------|---------|----------|------|
| 数据源注册 | `POST /api/v1/datasources` | `cdr.c_org` | 注册外部系统连接信息，标记连接状态 |
| 增量同步 | XXL-JOB 定时触发 / 手动 API | - | 基于时间戳增量拉取，避免全量同步 |
| 数据字典映射 | 内部映射服务 | `system.s_dict` | ICD-10/LOINC/SNOMED CT 标准编码映射 |
| 数据质量校验 | `POST /api/quality/rules` | `rdr.r_data_quality_rule` | 完整性/准确性/一致性/唯一性/时效性校验 |
| 写入 CDR | 内部批量写入 | `cdr.c_patient` / `cdr.c_encounter` 等 28 张表 | 采用 UPSERT（INSERT ON CONFLICT UPDATE）避免重复 |
| 缓存更新 | 内部更新 | Redis | 热点数据（患者、科室、医生）写入 Redis 缓存 |
| 同步日志 | 内部记录 | - | 记录本次同步的读取/写入/错误条数和耗时 |

---

## F5: ETL 数据转换管线

### 业务背景

ETL（Extract-Transform-Load）数据转换管线负责将 CDR 临床数据仓库中的标准化数据，按照科研项目的需求转换为研究数据（RDR）。流程包括选择 CDR 数据源、定义转换规则（字段映射、过滤条件、特征提取）、执行 ETL 任务、数据质量检测、写入 RDR Schema、生成数据集版本，最终通知研究员数据集已就绪。

### 流程图

```mermaid
flowchart TD
    Start([开始]) --> SelectCDR[选择 CDR 数据源\n指定患者队列/时间范围\n/科室/诊断等条件]
    SelectCDR --> DefineRule[定义转换规则\nrdr.r_etl_task.etl_config JSONB\n字段映射 / 过滤条件 / 特征提取]

    DefineRule --> ConfigSchedule{调度方式\nMANUAL / SCHEDULED}
    ConfigSchedule -->|手动| Execute[执行 ETL 任务\ndata-service 处理]
    ConfigSchedule -->|定时| CronJob[注册 XXL-JOB 任务\ncron_expression]
    CronJob --> Execute

    Execute --> Extract[Extract 抽取\n从 CDR Schema 读取数据\nSELECT FROM cdr.c_*]
    Extract --> Transform[Transform 转换\n字段重命名 / 类型转换\n特征计算 / 标签生成\n脱敏处理 / 编码标准化]

    Transform --> Load[Load 加载\n批量写入 RDR Schema]
    Load --> DQCheck{数据质量检测\nr_data_quality_rule}

    DQCheck -->|通过| WriteRDR[写入 RDR Schema\nr_clinical_feature\nr_imaging_dataset\nr_text_dataset 等]
    DQCheck -->|不通过| DQFail[记录质量报告\nr_data_quality_result\n标记失败样本]

    WriteRDR --> GenVersion[生成数据集版本\nrdr.r_dataset_version\n{version_no, record_count,\nfile_size, checksum=SHA256}]
    GenVersion --> UpdateStatus[更新数据集状态\nrdr.r_dataset.status = PUBLISHED]

    UpdateStatus --> Notify[通知研究员\nmsg-service 发送站内信\n/邮件/Webhook]
    Notify --> End([结束])

    DQFail --> ManualReview[人工审核质量报告]
    ManualReview --> DefineRule

    style Start fill:#4CAF50,color:#fff
    style End fill:#4CAF50,color:#fff
    style DQCheck fill:#FF9800,color:#fff
    style DQFail fill:#F44336,color:#fff
    style WriteRDR fill:#2196F3,color:#fff
    style GenVersion fill:#9C27B0,color:#fff
```

### 关键说明

| 步骤 | API 接口 | 数据库表 | 说明 |
|------|---------|----------|------|
| 定义转换规则 | `POST /api/etl/tasks` | `rdr.r_etl_task` | etl_config 为 JSONB，存放字段映射和过滤条件 |
| 执行 ETL | `POST /api/etl/tasks/{id}/execute` | `rdr.r_etl_task_log` | 记录每次执行的开始/结束时间、处理条数、错误数 |
| 数据质量检测 | 内部执行 | `rdr.r_data_quality_rule` + `rdr.r_data_quality_result` | 支持完整性/准确性/一致性/唯一性/时效性五类规则 |
| 写入 RDR | 内部批量写入 | `rdr.r_clinical_feature` / `rdr.r_imaging_dataset` / `rdr.r_text_dataset` | 多模态数据分别写入对应的子表 |
| 生成版本 | `POST /api/rdr/datasets/{id}/versions` | `rdr.r_dataset_version` | 语义化版本号，计算 SHA256 校验和 |
| 通知 | msg-service 异步 | - | ETL 完成后通过 msg-service 通知项目相关成员 |

---

## F6: 用户认证与鉴权流程

### 业务背景

MAIDC 采用 JWT + RBAC 的认证鉴权方案。用户登录时由 auth-service 验证密码并签发 JWT Token，Token 缓存在 Redis 中。后续请求经过 Gateway 统一拦截校验 JWT 有效性，再通过 RBAC 权限模型检查用户是否具备操作权限，最后通过 `org_id` 实现多租户数据权限隔离。

### 时序图

```plantuml
@startuml
title F6: 用户认证与鉴权流程
autonumber

actor "用户" as user
participant "Portal\n(Vue3)" as portal
participant "API Gateway\n(SCG)" as gateway
participant "auth-service\n(Java)" as auth
participant "Redis\n(缓存)" as redis
participant "model-service\n(Java)" as business

== 1. 用户登录 ==
user -> portal : 输入用户名/密码\n点击登录
portal -> gateway : POST /api/auth/login\n(username, password)
gateway -> auth : 路由转发

== 2. 密码验证 ==
auth -> auth : 查询 s_user\nWHERE username=?\nAND org_id=?
auth -> auth : BCrypt 密码比对\n检查账号锁定状态\n(连续5次失败锁定)
auth -> auth : 检查登录失败次数\n< 5 则允许
auth -> auth : 更新 last_login_at\nlast_login_ip

== 3. 签发 JWT Token ==
auth -> auth : 生成 Access Token\n(有效期 30 分钟,\npayload: user_id, username,\norg_id, roles)
auth -> auth : 生成 Refresh Token\n(有效期 7 天)
auth -> redis : 缓存 Token 信息\nmaidc:token:user_id\n(access_token, refresh_token,\nexpires_at)
redis --> auth : OK
auth --> gateway : 200 OK\n(access_token, refresh_token,\nexpires_in)
gateway --> portal : 返回 Token
portal -> portal : 存储 Token\n(localStorage)
portal --> user : 登录成功\n跳转首页

== 4. 业务请求鉴权 ==
user -> portal : 操作业务\n(如: 查看模型列表)
portal -> gateway : GET /api/v1/models\nHeader: Authorization\nBearer access_token

== 5. Gateway JWT 校验 ==
gateway -> gateway : 解析 Bearer Token
gateway -> gateway : 验证 JWT 签名\n(使用 HMAC-SHA256)
gateway -> gateway : 检查过期时间\nexp > now()
gateway -> redis : 检查 Token 黑名单\nmaidc:token:blacklist:token
redis --> gateway : 不在黑名单中\n(Token 有效)

== 6. RBAC 权限检查 ==
gateway -> gateway : 提取 user_id, roles\n从 JWT payload
gateway -> gateway : 匹配请求路径\n/api/v1/models ->\npermission: model:list
gateway -> redis : 查询角色权限缓存\nmaidc:role:role_id:permissions
redis --> gateway : 权限列表\n[CREATE, READ, UPDATE, ...]
gateway -> gateway : 检查权限\nmodel:list 是否在权限列表中

alt 权限不足
    gateway --> portal : 403 Forbidden\n"无访问权限"
    portal --> user : 提示无权限
end

== 7. 数据权限隔离 ==
gateway -> business : 转发请求\nHeader: X-User-Id, X-Org-Id,\nX-Roles
business -> business : MyBatis 拦截器\n自动附加 WHERE org_id = ?
note over business
  所有业务表的 org_id 字段
  由拦截器自动注入
  确保多租户数据隔离
end note

business -> business : 查询 m_model\nWHERE org_id = ?\nAND is_deleted = false
business --> gateway : 200 OK\n(模型列表数据)
gateway --> portal : 返回数据
portal --> user : 展示模型列表

== 8. Token 刷新 ==
portal -> gateway : POST /api/auth/refresh\n(refresh_token)
gateway -> auth : 路由转发
auth -> redis : 验证 refresh_token\nmaidc:token:user_id
redis --> auth : Token 有效
auth -> auth : 签发新 Access Token\n(有效期 30 分钟)
auth -> redis : 更新缓存\nmaidc:token:user_id
auth --> gateway : 200 OK\n(new_access_token, expires_in)
gateway --> portal : 返回新 Token
portal -> portal : 更新本地 Token

== 9. 用户登出 ==
user -> portal : 点击登出
portal -> gateway : POST /api/auth/logout\nHeader: Bearer access_token
gateway -> auth : 路由转发
auth -> redis : 加入 Token 黑名单\nmaidc:token:blacklist:token\n(TTL = Token 剩余有效期)
auth -> redis : 删除 Token 缓存\nmaidc:token:user_id
auth --> gateway : 200 OK
gateway --> portal : 登出成功
portal -> portal : 清除本地 Token
portal --> user : 跳转登录页

@enduml
```

### 关键说明

| 步骤 | API 接口 | 数据库表 | 说明 |
|------|---------|----------|------|
| 登录 | `POST /api/auth/login` | `system.s_user` | BCrypt 密码验证，连续 5 次失败锁定账号 |
| Token 签发 | 内部生成 | - | Access Token 30 分钟过期，Refresh Token 7 天过期 |
| Token 缓存 | - | Redis `maidc:token:{user_id}` | Token 信息缓存到 Redis，支持主动失效（登出/踢下线） |
| JWT 校验 | Gateway 全局过滤器 | Redis | 解析 JWT 验证签名和过期时间，检查 Redis 黑名单 |
| RBAC 权限 | Gateway 权限过滤器 | `system.s_user_role` / `system.s_role_permission` / `system.s_permission` | 用户 -> 角色 -> 权限三级模型，支持菜单/API/按钮/数据级权限 |
| 数据权限 | 业务服务拦截器 | 所有业务表的 `org_id` 字段 | 通过 MyBatis 拦截器自动附加 `WHERE org_id = ?` 条件 |
| Token 刷新 | `POST /api/auth/refresh` | - | 使用 Refresh Token 换取新 Access Token，无需重新登录 |

---

## F7: 数据标注工作流

### 业务背景

数据标注是 AI 模型训练数据准备的关键环节。MAIDC 支持多模态数据标注（影像/文本/基因组），通过 AI 辅助预标注提高标注效率，采用多人交叉标注和质量评估（Cohen's Kappa/IoU）保障标注质量，最终将高质量标注数据入库，供模型训练使用。

### 流程图

```mermaid
flowchart TD
    Start([开始]) --> CreateTask[创建标注任务\nPOST /api/v1/annotation-tasks\n指定数据集 + 标注类型\n+ 标注指南]
    CreateTask --> AssignAnnotator[分配标注员\n支持多人协同\n每人分配独立数据子集]

    AssignAnnotator --> AIPreLabel{启用AI预标注?}
    AIPreLabel -->|是| AIPre[AI预标注\naiworker Celery Worker\n目标检测 / 语义分割\n/ NER 实体识别]
    AIPreLabel -->|否| ManualLabel[人工标注]
    AIPre --> ManualLabel[人工标注\n在AI预标注基础上修正]

    ManualLabel --> LabelType{标注类型}
    LabelType -->|影像| ImagingLabel[影像标注\n矩形框 / 多边形\n/ 关键点 / 分割掩码\n格式: COCO/VOC/NIfTI]
    LabelType -->|文本| TextLabel[文本标注\nNER 实体标注\n/ 关系标注 / 分类\n表: rdr.r_text_annotation]
    LabelType -->|基因组| GenomicLabel[基因组标注\n变异位点标注\n/ 基因-疾病关联]

    ImagingLabel --> CrossReview[交叉审核\n多人标注同一样本\n计算一致性指标]
    TextLabel --> CrossReview
    GenomicLabel --> CrossReview

    CrossReview --> QualityEval{质量评估}
    QualityEval -->|影像| CalcIoU[计算 IoU\nIntersection over Union\nIoU >= 0.80 为合格]
    QualityEval -->|文本| CalcKappa[计算 Cohen's Kappa\n一致性系数\nKappa >= 0.75 为合格]

    CalcIoU --> QualityPass{质量达标?}
    CalcKappa --> QualityPass
    QualityPass -->|是| MergeLabel[合并标注结果\n取多数投票 / 加权平均\n写入最终标注]
    QualityPass -->|否| Reassign[重新分配标注\n通知标注员修正\n返回人工标注]

    MergeLabel --> SaveDB[标注入库\nrdr.r_imaging_annotation\n/ r_text_annotation\nis_verified = true\nverified_by = 专家ID]
    SaveDB --> UpdateProgress[更新标注进度\n通知任务创建人]
    UpdateProgress --> End([结束])

    Reassign --> ManualLabel

    style Start fill:#4CAF50,color:#fff
    style End fill:#4CAF50,color:#fff
    style AIPreLabel fill:#FF9800,color:#fff
    style QualityPass fill:#FF9800,color:#fff
    style QualityEval fill:#FF9800,color:#fff
    style SaveDB fill:#2196F3,color:#fff
    style AIPre fill:#9C27B0,color:#fff
```

### 关键说明

| 步骤 | API 接口 | 数据库表 | 说明 |
|------|---------|----------|------|
| 创建标注任务 | `POST /api/v1/annotation-tasks` | 自定义标注任务表 | 指定数据集、标注类型（BBOX/SEGMENTATION/NER/CLASSIFICATION）、标注指南 |
| AI 预标注 | RabbitMQ `preprocessing` 队列 | - | aiworker Celery Worker 调用预训练模型生成初始标注 |
| 影像标注 | 前端标注工具 | `rdr.r_imaging_annotation` | 支持矩形框、多边形、关键点、分割掩码，格式 COCO/VOC/NIfTI/DICOM-Seg |
| 文本标注 | 前端标注工具 | `rdr.r_text_annotation` | 支持 NER 实体标注、关系标注、分类，记录文本偏移量 |
| 交叉审核 | 内部逻辑 | - | 多人标注同一样本，计算一致性指标 |
| 质量评估 | 内部计算 | - | 影像用 IoU（>= 0.80），文本用 Cohen's Kappa（>= 0.75） |
| 标注入库 | `PUT /api/v1/annotations/{id}/verify` | `rdr.r_imaging_annotation` / `rdr.r_text_annotation` | 设置 `is_verified = true`，记录审核人 |

---

## F8: 告警触发与通知流程

### 业务背景

MAIDC 的告警体系基于 Prometheus 指标采集和自定义告警规则（`m_alert_rule`），实现对模型推理性能、资源利用率、系统可用性的全方位监控。当指标触发告警规则时，系统生成告警记录并通过多渠道（邮件/Webhook/站内信）分发通知，支持告警确认、自动恢复检测和解除告警的完整生命周期管理。

### 流程图

```mermaid
flowchart TD
    Start([开始]) --> Collect[指标采集\nPrometheus + 自定义埋点\n采集: QPS / 延迟P99\n/ GPU利用率 / 错误率]
    Collect --> Store[存储指标\nmodel.m_model_metric\ndeployment_id, metric_name,\nmetric_value, metric_time]

    Store --> RuleMatch{规则引擎匹配\nmodel.m_alert_rule}
    RuleMatch -->|未触发| Continue[继续采集\n等待下一个周期]
    RuleMatch -->|触发规则| CheckDuration{持续时长检查\nduration_sec}

    CheckDuration -->|未达到| Continue
    CheckDuration -->|已达到| FireAlert[触发告警\n写入 model.m_alert_record\nstatus = FIRING\nmetric_value, threshold, message]

    FireAlert --> Dispatch{通知分发\nmsg-service}
    Dispatch -->|邮件| Email[发送邮件通知\nSMTP -> 审批人/运维人]
    Dispatch -->|Webhook| Webhook[推送 Webhook\n企业微信 / 钉钉\n/ 自定义回调]
    Dispatch -->|站内信| InApp[站内消息推送\nWebSocket 实时推送\n到 Portal 前端]

    Email --> WaitAck[等待确认]
    Webhook --> WaitAck
    InApp --> WaitAck

    WaitAck --> AckCheck{是否确认?}
    AckCheck -->|人工确认| Ack[确认告警\nPUT /api/v1/alerts/id/acknowledge\nstatus -> ACKNOWLEDGED\nrecord acknowledged_by]
    AckCheck -->|未确认| Remind[持续提醒\n按 escalate 策略\n升级通知范围]

    Ack --> AutoRecover{自动恢复检测\n持续监控指标}
    Remind --> AutoRecover

    AutoRecover -->|指标恢复正常| Resolve[解除告警\nstatus -> RESOLVED\nrecord resolved_at]
    AutoRecover -->|指标未恢复| ContinueAlert[继续保持 FIRING\n持续通知\n等待恢复]
    ContinueAlert --> AutoRecover

    Resolve --> Log[记录告警处理日志\n计算 MTTR 平均恢复时间\n更新告警统计]
    Log --> End([结束])

    style Start fill:#4CAF50,color:#fff
    style End fill:#4CAF50,color:#fff
    style RuleMatch fill:#FF9800,color:#fff
    style FireAlert fill:#F44336,color:#fff
    style Resolve fill:#4CAF50,color:#fff
    style Dispatch fill:#9C27B0,color:#fff
    style Ack fill:#2196F3,color:#fff
```

### 关键说明

| 步骤 | API 接口 | 数据库表 | 说明 |
|------|---------|----------|------|
| 指标采集 | Prometheus + 自定义埋点 | `model.m_model_metric` | 采集 QPS、延迟 P50/P99、GPU 利用率、错误率等指标 |
| 规则匹配 | 内部定时检查 | `model.m_alert_rule` | 支持 GT/GTE/LT/LTE/EQ/NE 条件，可配置持续时长阈值 |
| 触发告警 | 内部生成 | `model.m_alert_record` | 状态 FIRING，记录触发时间、指标值、阈值、告警消息 |
| 通知分发 | msg-service 异步 | - | 支持邮件（SMTP）、Webhook（企业微信/钉钉）、站内信（WebSocket） |
| 确认告警 | `PUT /api/v1/alerts/{id}/acknowledge` | `model.m_alert_record` | 状态转为 ACKNOWLEDGED，记录确认人和时间 |
| 解除告警 | 内部检测 | `model.m_alert_record` | 指标恢复正常后自动解除，记录恢复时间，计算 MTTR |
| 持续提醒 | msg-service | - | 未确认的告警按升级策略扩大通知范围 |

**告警严重级别**:
- **CRITICAL**: 需要立即处理，如推理服务不可用、错误率 > 10%
- **WARNING**: 需要关注，如延迟 P99 > 500ms、GPU 利用率 > 90%
- **INFO**: 信息提示，如部署状态变更、配置修改

---

## 附录：流程与服务映射关系

| 流程编号 | 流程名称 | 主要参与服务 | 涉及 Schema |
|----------|----------|-------------|-------------|
| F1 | 模型全生命周期 | model-service, aiworker, msg-service, MinIO | model |
| F2 | 模型推理调用 | Gateway, model-service, aiworker, audit-service | model, audit |
| F3 | 金丝雀发布与流量路由 | model-service, aiworker | model |
| F4 | CDR 数据接入 | data-service, XXL-JOB | cdr, system |
| F5 | ETL 数据转换管线 | data-service, XXL-JOB | cdr, rdr |
| F6 | 用户认证与鉴权 | Gateway, auth-service, Redis | system |
| F7 | 数据标注工作流 | label-service, aiworker | rdr |
| F8 | 告警触发与通知 | model-service, msg-service, Prometheus | model |

---

> **文档结束** - MAIDC 交互流程图集 v1.0
